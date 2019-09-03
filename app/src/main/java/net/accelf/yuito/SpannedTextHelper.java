package net.accelf.yuito;

import android.text.SpannableStringBuilder;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SpannedTextHelper {

    static Spanned replaceSpanned(Spanned targetText) {
        String targetString = targetText.toString();
        SpannableStringBuilder builder = new SpannableStringBuilder(targetText);
        Pattern pattern = Pattern.compile("\n");
        Matcher matcher = pattern.matcher(targetString);
        while (matcher.find()) {
            builder.replace(matcher.start(), matcher.end(), " ");
        }
        return builder;
    }

}
