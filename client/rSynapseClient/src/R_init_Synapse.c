#include <Rdefines.h>
#include <R_ext/Rdynload.h>
#include "synapse_curl.h"

static const R_CallMethodDef callMethods[] = {
    {".writer_open", (DL_FUNC) &writer_open, 1},
    {".writer_close", (DL_FUNC) &writer_close, 1},
	{".reader_open", (DL_FUNC) &writer_open, 1},
    {".reader_close", (DL_FUNC) &writer_close, 1},
    {NULL, NULL, 0}
};

void
R_init_Synapse(DllInfo *info)
{
    R_registerRoutines(info, NULL, callMethods, NULL, NULL);
}

void
R_unload_Synapse(DllInfo *info)
{
    /* any clean-up when package unloaded */
}
