#!/usr/bin/env python

# A test file must start with a header, where each line starts with '// '.
# The first line of the header must be 'Plinth Test'.
# Successive lines can specify options for the test. Valid options are:
#   Main: <type name>
#     Uses the specified type name as the main type argument to the compiler.
#   Compiler: <some text>
#     Tests that the compiler outputs the specified text as a whole line, somewhere in its output.
#   Args: <some arguments>
#     Uses the specified argument list for running the compiled test program.
#   ReturnCode: <number>
#     Tests that running the program produces the specified return code.
#   Out: <some text>
#     Tests that the running compiled code produces exactly the specified text.
#     If multiple "Out: " lines are used, the output must contain all lines in the order specified.

import sys
from subprocess import Popen,PIPE,call
import os
import tempfile
import shutil

PLINTH_DIR = os.path.abspath(os.path.dirname(os.path.abspath(__file__)) + '/..')
COMPILER_EXEC = PLINTH_DIR + '/plinth'
RUNTIME = PLINTH_DIR + '/runtime/runtime.bc'
TEST_LIB = PLINTH_DIR + '/test/TestLib.pth'

class Test:
  def __init__(self, filename, mainTypeName, compilerLines, args, returnCode, outLines):
    self.filename = filename
    self.mainTypeName = mainTypeName
    self.compilerLines = compilerLines
    self.args = args
    self.returnCode = returnCode
    self.outLines = outLines

def parse_header(filename):
  lines = []
  with open(filename) as f:
    lines = f.readlines()
  header = []
  for i in range(len(lines)):
    if lines[i].startswith('// '):
      header.append(lines[i][3:])
    else:
      break
  if len(header) == 0:
    print("A test must contain a header")
    sys.exit(1)
  if header[0].strip() != "Plinth Test":
    print("A test must start with the plinth test header: \"// Plinth Test\"")
    sys.exit(1)
  mainTypeName = None
  compilerLines = []
  args = None
  returnCode = None
  outLines = None
  for i in range(1, len(header)):
    if header[i].startswith('Main: '):
      if mainTypeName != None:
        print("A test cannot declare two main types")
        sys.exit(1)
      mainTypeName = header[i][len('Main: '):].strip()
    elif header[i].startswith('Compiler: '):
      compilerLines.append(header[i][len('Compiler: '):])
    elif header[i].startswith('Args: '):
      if args != None:
        print("A test cannot declare two argument lists")
        sys.exit(1)
      args = header[i][len('Args: '):].strip().split(' ')
    elif header[i].startswith('ReturnCode: '):
      if returnCode != None:
        print("A test cannot declare two return codes")
        sys.exit(1)
      returnCode = int(header[i][len('ReturnCode: '):].strip())
    elif header[i].startswith('Out: '):
      if outLines == None:
        outLines = []
      outLines.append(header[i][len('Out: '):])
    elif header[i].strip() != '':
      print("Unrecognised header line: " + header[i])
      sys.exit(1)
  if mainTypeName == None and returnCode != None:
    print("To test for a return code, please specify \"Main: \"")
    sys.exit(1)
  if mainTypeName == None and outLines != None:
    print("To test an executable's output, please specify \"Main: \"")
    sys.exit(1)
  if mainTypeName == None and args != None:
    print("Cannot provide arguments to a test without a main type")
    sys.exit(1)
  if args != None and returnCode == None and outLines == None:
    print("Cannot provide arguments to a test with no expected results")
    sys.exit(1)
  if compilerLines == [] and returnCode == None and outLines == None:
    print("No tests specified!")
    sys.exit(1)
  return Test(filename, mainTypeName, compilerLines, args, returnCode, outLines)

def run_compiler(test):
  work_dir = test.work_dir
  test.compiled_base = work_dir + "/compiled"
  if test.mainTypeName != None:
    test.compiled_base = work_dir + "/" + test.mainTypeName
  bitcode_file = test.compiled_base + ".pbc"

  args = [COMPILER_EXEC, '-o', bitcode_file, '-l', RUNTIME]
  if test.mainTypeName != None:
    args += ['-m', test.mainTypeName]
  args += [TEST_LIB]
  args += [test.filename]
  proc = Popen(args, stderr=PIPE)
  stdout, stderr = proc.communicate()


  error_lines = stderr.decode('utf-8').splitlines(True)
  basename = os.path.basename(test.filename)
  compilerLines = [x.replace(basename, test.filename) for x in test.compilerLines]
  for i in range(len(error_lines)):
    for j in range(len(compilerLines)):
      if error_lines[i].strip() == compilerLines[j].strip():
        del compilerLines[j]
        break

  if len(compilerLines) > 0:
    print("Fail: " + test.filename)
    print("  Failed to produce the compiler output lines:")
    for i in range(len(compilerLines)):
      print("    " + compilerLines[i].strip())
    print("  Actual compiler output was:")
    for i in range(len(error_lines)):
      print("  > " + error_lines[i], end="")
    shutil.rmtree(work_dir)
    sys.exit(2)

  if proc.returncode != 0:
    if test.returnCode != None or test.outLines != None:
      print("Fail: " + test.filename)
      print("  Failed to compile! Compiler output:")
      for i in range(len(error_lines)):
        print("  > " + error_lines[i], end="")
      shutil.rmtree(work_dir)
      sys.exit(2)
    else:
      # Success, since the test must have been on the compiler output rather than the runtime output
      return


  assembly_file = test.compiled_base + ".s"
  verifyResult = call(['llc', bitcode_file, '-o', assembly_file])
  if verifyResult != 0:
    print("Fail: " + test.filename)
    print("  Failed to generate assembly from source file")
    shutil.rmtree(work_dir)
    sys.exit(2)

  executable_file = test.compiled_base
  gccResult = call(['gcc', assembly_file, '-o', executable_file])
  if gccResult != 0:
    print("Fail: " + test.filename)
    print("  Failed to generate an executable from the assembly code")
    shutil.rmtree(work_dir)
    sys.exit(2)

def run_program(test):
  work_dir = test.work_dir
  executable_file = test.compiled_base

  args = test.args
  if args == None:
    args = [test.mainTypeName]

  proc = Popen(args, executable=executable_file, stdout=PIPE)
  stdout, stderr = proc.communicate()

  if test.returnCode != None:
    if proc.returncode != test.returnCode:
      print("Fail: " + test.filename)
      print("  Return code mismatch. Expected: " + str(test.returnCode) + " but got: " + str(proc.returncode))
      shutil.rmtree(work_dir)
      sys.exit(3)


  output_lines = stdout.decode('utf-8').splitlines(True)
  if test.outLines != None:
    if len(output_lines) != len(test.outLines):
      print("Fail: " + test.filename)
      print("  Number of output lines differs (expected: " + str(len(test.outLines)) + " but got: " + str(len(output_lines)) + ")")
      shutil.rmtree(work_dir)
      sys.exit(3)
    for i in range(len(output_lines)):
      if output_lines[i] != test.outLines[i]:
        print("Fail: " + test.filename)
        print("  Output line differs. Expected:")
        print("    " + test.outLines[i], end="")
        print("  But got:")
        print("    " + output_lines[i], end="")
        shutil.rmtree(work_dir)
        sys.exit(3)


if __name__ == "__main__":
  filename = sys.argv[1]
  test = parse_header(filename)
  test.work_dir = tempfile.mkdtemp()
  run_compiler(test)
  if test.mainTypeName != None and (test.returnCode != None or test.outLines != None):
    run_program(test)
  # Success!
  print("Passed: " + test.filename)
  shutil.rmtree(test.work_dir)

