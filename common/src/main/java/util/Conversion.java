package util;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Conversion {
    private Conversion() {

    }

    public static String convertAmountToString(float amount) {
        DecimalFormat df = new DecimalFormat("#.000000");
        df.setRoundingMode(RoundingMode.DOWN);

        return df.format(amount);
    }
}
