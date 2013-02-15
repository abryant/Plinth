// for debugging only
#include <stdio.h>

#include <unwind.h>
#include <stddef.h>
#include <stdint.h>
#include <stdbool.h>
#include <stdlib.h>
#include <string.h>

static const uint64_t plinthExceptionClass = 0x414A420050544800; // AJB\0PTH\0

typedef struct exception_handler
{
  uintptr_t landingPad;
  uintptr_t typeInfoIndex;
} exception_handler;

// DWARF exception header encoding constants
enum
{
  DW_EH_PE_absptr   = 0x00,
  DW_EH_PE_uleb128  = 0x01,
  DW_EH_PE_udata2   = 0x02,
  DW_EH_PE_udata4   = 0x03,
  DW_EH_PE_udata8   = 0x04,
  DW_EH_PE_sleb128  = 0x09,
  DW_EH_PE_sdata2   = 0x0A,
  DW_EH_PE_sdata4   = 0x0B,
  DW_EH_PE_sdata8   = 0x0C,
  DW_EH_PE_pcrel    = 0x10,
  DW_EH_PE_textrel  = 0x20,
  DW_EH_PE_datarel  = 0x30,
  DW_EH_PE_funcrel  = 0x40,
  DW_EH_PE_aligned  = 0x50,
  DW_EH_PE_indirect = 0x80,
  DW_EH_PE_omit     = 0xFF
};

typedef struct _Unwind_Exception _Unwind_Exception;
typedef struct _Unwind_Context _Unwind_Context;

// defined in core/exception.ll
bool plinth_exception_instanceof(const uint8_t *plinthObject, const uint8_t *typeInfo);
// defined in plinth-src/Throwable.pth
void plinth_print_uncaught_exception(const uint8_t *plinthException);

void* plinth_create_exception(const uint8_t *plinthExceptionObject);
void plinth_throw(_Unwind_Exception *exception);
_Unwind_Reason_Code plinth_personality(int version, _Unwind_Action actions, uint64_t exceptionClass,
                                       _Unwind_Exception *exception, _Unwind_Context *context);
const uint8_t* plinth_catch(_Unwind_Exception *exception);
static void plinth_destroy_exception(_Unwind_Reason_Code reason, _Unwind_Exception *exception);
static const uint8_t* extractExceptionObject(_Unwind_Exception *exception);
static void setRegisters(_Unwind_Exception *exception, _Unwind_Context *context, exception_handler *handler);
static bool findHandler(exception_handler *resultHandler, bool findCleanup, bool isForeign, _Unwind_Exception *exception, _Unwind_Context *context);
static bool exceptionSpecificationMatches(const uint8_t *typeInfoTable, uint8_t typeInfoTableEncoding, intptr_t specIndex, _Unwind_Exception *exception);
static const uint8_t* getTypeInfo(const uint8_t *typeInfoTable, uint8_t typeInfoTableEncoding, uintptr_t typeInfoIndex);
static uintptr_t readULEB128(const uint8_t **data);
static uintptr_t readSLEB128(const uint8_t **data);
static uintptr_t readEncoded(const uint8_t **data, uint8_t encoding);

static void debug_abort(const char *str)
{
  fprintf(stderr, "Exception handling error: %s\n", str);
  fflush(stderr);
  abort();
}

void* plinth_create_exception(const uint8_t *plinthExceptionObject)
{
  size_t size = sizeof(_Unwind_Exception) + sizeof(uint8_t*);
  uint8_t *memory = calloc(1, size);
  if (memory == 0)
  {
    // out of memory, cannot create _Unwind_Exception
    debug_abort("out-of-memory");
  }
  _Unwind_Exception *exception = (_Unwind_Exception*) memory;
  exception->exception_class = plinthExceptionClass;
  exception->exception_cleanup = plinth_destroy_exception;

  // store the plinth exception immediately after this exception object
  const uint8_t** dataPointer = (const uint8_t**) (exception + 1);
  *dataPointer = plinthExceptionObject;
  return exception;
}

void plinth_throw(_Unwind_Exception *exception)
{
  _Unwind_Reason_Code reason = _Unwind_RaiseException(exception);
  if (reason == _URC_END_OF_STACK)
  {
    const uint8_t *plinthException = extractExceptionObject(exception);
    plinth_destroy_exception(0, exception);
    plinth_print_uncaught_exception(plinthException);
    fflush(stdout);
    fflush(stderr);
    abort();
  }

  // the exception has been raised, so if we reach here something has gone wrong
  fprintf(stderr, "failed to throw exception! reason=%d\n", reason);
  debug_abort("throw-failed");
}

const uint8_t* plinth_catch(_Unwind_Exception *exception)
{
  const uint8_t *plinthExceptionObject = extractExceptionObject(exception);
  plinth_destroy_exception(0, exception);
  return plinthExceptionObject;
}

static void plinth_destroy_exception(_Unwind_Reason_Code reason, _Unwind_Exception *exception)
{
  (void) reason;
  free(exception);
}

static const uint8_t* extractExceptionObject(_Unwind_Exception *exception)
{
  uint8_t** dataPointer = (uint8_t**) (exception + 1);
  return *dataPointer;
}

_Unwind_Reason_Code plinth_personality(int version, _Unwind_Action actions, uint64_t exceptionClass,
                                       _Unwind_Exception *exception, _Unwind_Context *context)
{
  if (version != 1 || exception == 0 || context == 0)
  {
    return _URC_FATAL_PHASE1_ERROR;
  }
  bool isForeign = exceptionClass != plinthExceptionClass;
  if (actions & _UA_SEARCH_PHASE)
  {
    // Phase 1: Check whether the current frame contains a handler that stops the unwinding process
    // If we find a handler, we must return _URC_HANDLER_FOUND
    // If not, we must return _URC_CONTINUE_UNWIND

    // first, check that the flags aren't invalid
    if (actions & (_UA_CLEANUP_PHASE | _UA_HANDLER_FRAME | _UA_FORCE_UNWIND))
    {
      return _URC_FATAL_PHASE1_ERROR;
    }
    // now search for a handler
    exception_handler result;
    if (findHandler(&result, false, isForeign, exception, context))
    {
      return _URC_HANDLER_FOUND;
    }
    return _URC_CONTINUE_UNWIND;
  }
  if (actions & _UA_CLEANUP_PHASE)
  {
    // Phase 2: Perform cleanup for the current frame, or set the registers to perform it.
    // If we perform cleanup ourselves, we must return _URC_CONTINUE_UNWIND
    // If we just set up the registers, we must return _URC_INSTALL_CONTEXT
    // In this implementation, we just set up the registers

    // first, check that the flags aren't invalid
    if ((actions & _UA_HANDLER_FRAME) && (actions & _UA_FORCE_UNWIND))
    {
      return _URC_FATAL_PHASE2_ERROR;
    }
    // now process the cleanup
    if (actions & _UA_HANDLER_FRAME)
    {
      // find the handler that phase 1 found
      exception_handler result;
      if (!findHandler(&result, false, isForeign, exception, context))
      {
        // phase 1 said we had a handler, but there isn't one!
        debug_abort("phase-1-lied");
      }
      // set the registers and return to the unwinder to run the handler
      setRegisters(exception, context, &result);
      return _URC_INSTALL_CONTEXT;
    }

    // either this is a forced unwind, or phase 1 didn't find any handlers
    // so search for a cleanup
    exception_handler result;
    if (findHandler(&result, true, isForeign, exception, context))
    {
      setRegisters(exception, context, &result);
      return _URC_INSTALL_CONTEXT;
    }
    return _URC_CONTINUE_UNWIND;
  }
  // bad arguments: treat this case as a phase 1 error
  return _URC_FATAL_PHASE1_ERROR;
}

static void setRegisters(_Unwind_Exception *exception, _Unwind_Context *context, exception_handler *handler)
{
  _Unwind_SetGR(context, __builtin_eh_return_data_regno(0), (uintptr_t) exception);
  _Unwind_SetGR(context, __builtin_eh_return_data_regno(1), (uintptr_t) handler->typeInfoIndex);
  _Unwind_SetIP(context, handler->landingPad);
}

// Scans the DWARF exception table for a handler for the specified exception
static bool findHandler(exception_handler *resultHandler, bool findCleanup, bool isForeign, _Unwind_Exception *exception, _Unwind_Context *context)
{
  // get the language specific data area
  const uint8_t *lsda = (const uint8_t*) _Unwind_GetLanguageSpecificData(context);
  if (lsda == 0)
  {
    // this frame doesn't have an exception table
    return false;
  }
  // Get the current instruction pointer in the unwinding context
  // According to the Itanium C++ ABI:
  //   During unwinding, this is guaranteed to be the address of the bundle immediately
  //   following the call site in the function identified by the unwind context
  // We subtract 1 to get the pointer to the call instruction,
  // which we will use to search through the exception table.
  uintptr_t instructionPointer = _Unwind_GetIP(context) - 1;

  // Get the pointer to the beginning of the frame's code
  uintptr_t functionStart = _Unwind_GetRegionStart(context);
  // find the index of the instruction in the function
  uintptr_t instructionIndex = instructionPointer - functionStart;

  // read the location of the start of the landing pad
  uint8_t lpStartEncoding = *lsda;
  lsda++;
  uintptr_t lpStart = readEncoded(&lsda, lpStartEncoding);
  if (lpStart == 0)
  {
    lpStart = functionStart;
  }

  // read the encoding of the type info table
  uint8_t typeInfoTableEncoding = *lsda;
  lsda++;
  // read the offset to the type info table
  const uint8_t *typeInfoTable = 0;
  if (typeInfoTableEncoding != DW_EH_PE_omit)
  {
    uintptr_t typeInfoTableOffset = readULEB128(&lsda);
    typeInfoTable = lsda + typeInfoTableOffset;
  }

  // read the call site table's encoding
  uint8_t callSiteEncoding = *lsda;
  lsda++;
  // read the call site table's length
  uintptr_t callSiteTableLength = readULEB128(&lsda);

  // search through the call site table for a site which contains the current instruction pointer
  const uint8_t *callSiteTablePointer = lsda;
  const uint8_t *actionTable = lsda + callSiteTableLength;
  while (callSiteTablePointer < actionTable)
  {
    // read the current call site table entry
    uintptr_t start = readEncoded(&callSiteTablePointer, callSiteEncoding);
    uintptr_t length = readEncoded(&callSiteTablePointer, callSiteEncoding);
    uintptr_t landingPad = readEncoded(&callSiteTablePointer, callSiteEncoding);
    uintptr_t actionIndex = readULEB128(&callSiteTablePointer);

    // check whether the instruction pointer indexes into this entry
    if ((start <= instructionIndex) && (instructionIndex < (start + length)))
    {
      // we have found the call site, so calculate the landing pad's location
      if (landingPad == 0)
      {
        return false;
      }
      landingPad += (uintptr_t) lpStart;

      // if this is a cleanup entry, return
      if (actionIndex == 0)
      {
        if (findCleanup)
        {
          resultHandler->landingPad = landingPad;
          resultHandler->typeInfoIndex = 0; // cleanup
          return true;
        }
        return false;
      }

      // search for a matching action entry
      const uint8_t *action = actionTable + (actionIndex - 1);
      while (true)
      {
        intptr_t typeInfoIndex = readSLEB128(&action);
        if (typeInfoIndex > 0)
        {
          const uint8_t *typeInfo = getTypeInfo(typeInfoTable, typeInfoTableEncoding, (uintptr_t) typeInfoIndex);
          if (typeInfo == 0)
          {
            // null type info indicates a handler which catches everything, including foreign exceptions
            resultHandler->landingPad = landingPad;
            resultHandler->typeInfoIndex = typeInfoIndex;
            return true;
          }
          if (!isForeign)
          {
            const uint8_t *plinthObject = extractExceptionObject(exception);
            if (plinth_exception_instanceof(plinthObject, typeInfo))
            {
              resultHandler->landingPad = landingPad;
              resultHandler->typeInfoIndex = typeInfoIndex;
              return true;
            }
          }
        }
        else if (typeInfoIndex < 0)
        {
          // this is an exception specification, which means that we should continue
          // unwinding for any exceptions in the list, but catch any which are not
          if (isForeign)
          {
            // foreign exceptions are always caught
            resultHandler->landingPad = landingPad;
            resultHandler->typeInfoIndex = typeInfoIndex;
            return true;
          }
          if (!exceptionSpecificationMatches(typeInfoTable, typeInfoTableEncoding, typeInfoIndex, exception))
          {
            // there is no specification for this in the exception specs list, so catch it
            resultHandler->landingPad = landingPad;
            resultHandler->typeInfoIndex = typeInfoIndex;
            return true;
          }
        }
        else // typeInfoIndex == 0
        {
          // this is a cleanup entry, so return
          if (findCleanup)
          {
            resultHandler->landingPad = landingPad;
            resultHandler->typeInfoIndex = typeInfoIndex;
            return true;
          }
          return false;
        }

        const uint8_t *nextActionOffsetPointer = action;
        intptr_t nextActionOffset = readSLEB128(&action);
        if (nextActionOffset == 0)
        {
          // we have reached the end of the actions list, so return that we haven't found a handler
          return false;
        }
        action = nextActionOffsetPointer + nextActionOffset;
      }
    }
    else if (instructionIndex < start)
    {
      // there isn't a call site for this instruction
      debug_abort("no-call-site");
    }
  }

  // nothing specifies how to handle this exception
  debug_abort("end-scan");
  abort();
}

// Checks whether the specified exception specification can catch the specified exception
static bool exceptionSpecificationMatches(const uint8_t *typeInfoTable, uint8_t typeInfoTableEncoding, intptr_t specIndex, _Unwind_Exception *exception)
{
  if (typeInfoTable == 0 || specIndex >= 0)
  {
    debug_abort("exception-spec");
  }
  const uint8_t *specListPointer = typeInfoTable - 1 - specIndex;
  while (true)
  {
    uintptr_t specTypeInfoIndex = readULEB128(&specListPointer);
    if (specTypeInfoIndex == 0)
    {
      break;
    }
    const uint8_t *specTypeInfo = getTypeInfo(typeInfoTable, typeInfoTableEncoding, specTypeInfoIndex);
    if (specTypeInfo == 0)
    {
      // this is a catch-all, which always matches
      return true;
    }
    const uint8_t *plinthObject = extractExceptionObject(exception);
    if (plinth_exception_instanceof(plinthObject, specTypeInfo))
    {
      return true;
    }
  }
  return false;
}

// Gets the plinth RTTI pointer inside the specified index in the type info table
static const uint8_t* getTypeInfo(const uint8_t *typeInfoTable, uint8_t typeInfoTableEncoding, uintptr_t typeInfoIndex)
{
  if (typeInfoTable == 0)
  {
    // there is no type table, so abort
    debug_abort("typeinfo1");
  }
  switch (typeInfoTableEncoding & 0x0F)
  {
    case DW_EH_PE_absptr:
      typeInfoIndex *= sizeof(void*);
      break;
    case DW_EH_PE_udata2:
    case DW_EH_PE_sdata2:
      typeInfoIndex *= 2;
      break;
    case DW_EH_PE_udata4:
    case DW_EH_PE_sdata4:
      typeInfoIndex *= 4;
      break;
    case DW_EH_PE_udata8:
    case DW_EH_PE_sdata8:
      typeInfoIndex *= 8;
      break;
    default:
      // no other encodings are supported
      debug_abort("typeinfo2");
  }
  typeInfoTable -= typeInfoIndex;
  return (const uint8_t*) readEncoded(&typeInfoTable, typeInfoTableEncoding);
}

static uintptr_t readULEB128(const uint8_t **data)
{
  uintptr_t result = 0;
  uintptr_t shift = 0;
  while (true)
  {
    uint8_t byte = **data;
    (*data)++;
    result |= ((byte & 0x7f) << shift);
    shift += 7;
    if ((byte & 0x80) == 0)
    {
      break;
    }
  }
  return result;
}

static uintptr_t readSLEB128(const uint8_t **data)
{
  uintptr_t result = 0;
  uintptr_t shift = 0;
  uint8_t byte = 0;
  while (true)
  {
    byte = **data;
    (*data)++;
    result |= ((byte & 0x7f) << shift);
    shift += 7;
    if ((byte & 0x80) == 0)
    {
      break;
    }
  }
  if ((shift < sizeof(result) * 8) && (byte & 0x40))
  {
    result |= -(1 << shift);
  }
  return (intptr_t) result;
}

static uintptr_t readEncoded(const uint8_t **data, uint8_t encoding)
{
  if (encoding == DW_EH_PE_omit)
  {
    return 0;
  }
  uintptr_t result = 0;
  switch (encoding)
  {
    case DW_EH_PE_absptr:
      result = *((const uintptr_t*) *data);
      *data += sizeof(uintptr_t);
      break;
    case DW_EH_PE_uleb128:
      result = readULEB128(data);
      break;
    case DW_EH_PE_udata2:
      result = (uintptr_t) *((const uint16_t*) *data);
      *data += sizeof(uint16_t);
      break;
    case DW_EH_PE_udata4:
      result = (uintptr_t) *((const uint32_t*) *data);
      *data += sizeof(uint32_t);
      break;
    case DW_EH_PE_udata8:
      result = (uintptr_t) *((const uint64_t*) *data);
      *data += sizeof(uint64_t);
      break;
    case DW_EH_PE_sleb128:
      result = (uintptr_t) readSLEB128(data);
      break;
    case DW_EH_PE_sdata2:
      result = (uintptr_t) *((const int16_t*) *data);
      *data += sizeof(int16_t);
      break;
    case DW_EH_PE_sdata4:
      result = (uintptr_t) *((const int32_t*) *data);
      *data += sizeof(int32_t);
      break;
    case DW_EH_PE_sdata8:
      result = (uintptr_t) *((const int64_t*) *data);
      *data += sizeof(int64_t);
      break;
    default:
      // no other encodings are supported
      debug_abort("encoding1");
  }
  // these encodings specify an offset to give to the value we've just read
  switch (encoding & 0x70)
  {
    case DW_EH_PE_absptr:
      // the pointer is absolute, so don't add anything on
      break;
    case DW_EH_PE_pcrel:
      if (result)
      {
        result += (uintptr_t) *data;
      }
      break;
    case DW_EH_PE_textrel:
    case DW_EH_PE_datarel:
    case DW_EH_PE_funcrel:
    case DW_EH_PE_aligned:
    default:
      // these encodings are not supported
      debug_abort("encoding2");
  }
  if (((encoding & DW_EH_PE_indirect) == DW_EH_PE_indirect) && result)
  {
    result = *((uintptr_t*) result);
  }
  return result;
}


