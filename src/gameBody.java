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

//血条显示有问题，角色是绿色血条
//boss战召唤物有血条，是原本怪物学的一半，相当于是boss的护盾，只有击败站换物，才能对boss造成伤害
//选择一个随机事件，其他的事件不能用以访问的标签
//所有的内容在一页里呈现，不要往下滑
//造物主数值还是高了，再削一点数值倍率
//超级噩梦boss强度
//怪物血量比角色少前几层
