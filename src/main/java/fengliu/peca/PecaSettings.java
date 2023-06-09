package fengliu.peca;

import carpet.api.settings.Rule;

public class PecaSettings {

    public static final String PECA = "PECA";

    @Rule(
        categories = {PECA}
    )
    public static boolean playerReset = false;

}
