package app.reelvocab.core.domain.model

class DictionaryAiException(
    message: String = "AI dictionary lookup failed",
    cause: Throwable? = null,
) : Exception(message, cause)
