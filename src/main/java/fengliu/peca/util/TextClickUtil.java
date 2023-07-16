package fengliu.peca.util;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;

public class TextClickUtil {

    public static MutableText runText(MutableText text, String command){
        return text.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command)));
    }

    public static MutableText suggestText(MutableText text, String command){
        return text.setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
    }
}
