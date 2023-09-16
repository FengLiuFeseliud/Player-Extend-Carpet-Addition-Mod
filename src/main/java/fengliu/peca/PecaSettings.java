package fengliu.peca;

import carpet.api.settings.Rule;

import static carpet.api.settings.RuleCategory.COMMAND;

public class PecaSettings {

    public static final String PECA = "PECA";

    @Rule(categories = {PECA})
    public static boolean groupCanBePlayerLogInSpawn = false;

    @Rule(categories = {PECA, COMMAND})
    public static String commandPlayerGroup = "ops";

    @Rule(categories = {PECA, COMMAND})
    public static String commandPlayerAuto = "ops";

    @Rule(categories = {PECA, COMMAND})
    public static String commandPlayerManage = "ops";

//    @Rule(categories = {PECA, COMMAND})
//    public static String commandPlayerGroupManage = "ops";

    @Rule(categories = {PECA, COMMAND})
    public static boolean hiddenFakePlayerGroupJoinMessage = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerGameModeLockSurvive = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerCanWalkOnFluid = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerCanNotPickUpItem = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerCanNotAssimilateExp = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerCanNotSurroundExp = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerImmuneInFireDamage = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerImmuneOnFireDamage = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerImmuneLavaDamage = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerImmuneDamage = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerImmunePlayerDamage = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerImmuneExplosionDamage = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerImmuneCrammingDamage = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerNotDie = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerNotHunger = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerNotHypoxic = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerNotBeCaughtInPowderSnow = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerWillNotAffectedByBubbleColumn = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerDropAllExp = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerDropExpNoUpperLimit = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerKeepInventory = false;

    @Rule(categories = {PECA})
    public static boolean playerReplaceLowTool = false;

    @Rule(categories = {PECA})
    public static boolean playerDropLowTool = false;
}
