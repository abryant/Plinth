#include <stdio.h>

int plinth_stdin_getc()
{
  return fgetc(stdin);
}

int plinth_stdout_putc(int c)
{
  return fputc(c, stdout);
}

int plinth_stderr_putc(int c)
{
  return fputc(c, stderr);
}

