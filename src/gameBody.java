import abyss.core.GameSession;

public final class gameBody {
    private gameBody() { }
    public static void main(String[] args) {
        try (var lock = abyss.save.SaveSessionLock.acquire()) {
            if (lock != null) {
                abyss.ui.Language.load();
                new GameSession().run();
            }
        }
    }
}
