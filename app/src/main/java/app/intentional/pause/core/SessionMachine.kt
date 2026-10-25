package app.intentional.pause.core

/** One owner and one approval; duplicate window events never begin a second session. */
class SessionMachine {
    var app: String? = null
        private set
    var approved = false
        private set
    var interrupted = false
        private set
    val counting get() = app != null && approved && !interrupted
    val prompting get() = app != null && !approved && !interrupted

    fun foreground(pkg: String, monitored: Boolean) {
        if (!monitored) { leave(); return }
        if (app != pkg) { app = pkg; approved = false }
        interrupted = false
    }
    fun approve(): Boolean {
        if (!prompting) return false
        approved = true
        return true
    }
    fun interrupt() { interrupted = true }
    fun leave() { app = null; approved = false; interrupted = false }
}
