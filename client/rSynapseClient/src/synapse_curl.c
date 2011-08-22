#include <Rdefines.h>
#include <stdio.h>

/* writer_open and writer_close manage the 'external pointer' that
 * references the C-level pointer to the opened file. _writer_finalize
 * is called explicitly or implicitly (when the external pointer is
 * garbage collected) to close the file.
 */

static void
_writer_finalizer(SEXP ext)
{
    if (NULL == R_ExternalPtrAddr(ext))
        return;
    FILE *file = (FILE *) R_ExternalPtrAddr(ext);
    int res = fclose(file);
    if (0 != res) {
        /* FIXME: errno */
        Rf_error("'writer' internal: failed to close");
    }
    R_SetExternalPtrAddr(ext, NULL);
}

static void
_reader_finalizer(SEXP ext)
{
    if (NULL == R_ExternalPtrAddr(ext))
        return;
    FILE *file = (FILE *) R_ExternalPtrAddr(ext);
    int res = fclose(file);
    if (0 != res) {
        /* FIXME: errno */
        Rf_error("'reader' internal: failed to close");
    }
    R_SetExternalPtrAddr(ext, NULL);
}

SEXP
writer_open(SEXP filename)
{
    FILE *file;
    SEXP ext;

    if (!isString(filename) || 1 != Rf_length(filename))
        Rf_error("'filename' must be character(1)");

    file = fopen(translateChar(STRING_ELT(filename, 0)), "wb");
    if (NULL == file) {
        /* FIXME: errno */
        Rf_error("'writer' failed to open file '%s'",
                 translateChar(STRING_ELT(filename, 0)));
    }

    ext = PROTECT(R_MakeExternalPtr(file, R_NilValue, R_NilValue));
    R_RegisterCFinalizerEx(ext, _writer_finalizer, TRUE);
    UNPROTECT(1);
    return ext;
}

SEXP
reader_open(SEXP filename)
{
	FILE *file;
	SEXP ext;
	
	if (!isString(filename) || 1 != Rf_length(filename))
        Rf_error("'filename' must be character(1)");
	
    file = fopen(translateChar(STRING_ELT(filename, 0)), "rb");
    if (NULL == file) {
        /* FIXME: errno */
        Rf_error("'writer' failed to open file '%s'",
                 translateChar(STRING_ELT(filename, 0)));
    }
	
	ext = PROTECT(R_MakeExternalPtr(file, R_NilValue, R_NilValue));
    R_RegisterCFinalizerEx(ext, _writer_finalizer, TRUE);
    UNPROTECT(1);
    return ext;
}

SEXP
writer_close(SEXP ext)
{
    _writer_finalizer(ext);
    return R_NilValue;
}

SEXP
reader_close(SEXP ext)
{
    _reader_finalizer(ext);
    return R_NilValue;
}

/* _writer_write is called by curl to write the data
 */

size_t
_writer_write(void *buffer, size_t size, size_t nmemb, void *data)
{
    FILE *file = (FILE *) data;
    size_t len;

    if (NULL == file) {
        /* Rf_warn signals something wrong but allows the curl library
         * to recover.
         *
         * FIXME: wonder what happens when curl library errors?
         */
        Rf_warning("'writer' internal: NULL FILE pointer");
        return 0;              /* trigger error in curl? */
    }

    len  = fwrite(buffer, size, nmemb, file);
    if (len != nmemb)
        /* Rf_warning here; error in curl library.
         *
         * FIXME: wonder what happens when curl library errors?
         */
        Rf_warning("'writer' internal: bytes written != bytes in buffer");

    return len;
}


size_t
_reader_read(void *buffer, size_t size, size_t nmemb, void *data)
{
	FILE *file = (FILE *) data;
	size_t len;
	
	if (NULL == file) {
        /* Rf_warn signals something wrong but allows the curl library
         * to recover.
         *
         * FIXME: wonder what happens when curl library errors?
         */
        Rf_warning("'reader' internal: NULL FILE pointer");
        return 0;              /* trigger error in curl? */
    }
	
	len = fread(buffer, size, nmemb, file);
	/*
	 * FIXME: is there a way to check that the read worked?
	 */
	
    return len;
}

