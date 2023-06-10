package fengliu.peca;

import carpet.api.settings.Rule;

import static carpet.api.settings.RuleCategory.COMMAND;

public class PecaSettings {

    public static final String PECA = "PECA";

    @Rule(categories = {PECA})
    public static boolean groupCanBePlayerLogInSpawn = false;

    @Rule(categories = {PECA, COMMAND})
    public static String commandPlayerGroup = "ops";

    @Rule(categories = {PECA})
    public static boolean fakePlayerGameModeLockSurvive = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerCanNotPickUpItem = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerCanNotAssimilateExp = false;

    @Rule(categories = {PECA})
    public static boolean fakePlayerCanNotSurroundExp = false;
}
