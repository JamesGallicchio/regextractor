package regex;

import java.lang.invoke.*;
import java.util.regex.Pattern;

public final class Bootstrap {
    private Bootstrap() {
    }

    /** Pre-compile a regex */
    public static CallSite bootstrap(MethodHandles.Lookup lookup, String invokedName,
                                     MethodType invokedType,
                                     String pattern) throws Throwable {
    	MethodHandle Pattern_compile = MethodHandles.lookup().findStatic(java.util.regex.Pattern.class, "compile", MethodType.fromMethodDescriptorString("(Ljava/lang/String;)Ljava/util/regex/Matcher;", lookup.lookupClass().getClassLoader()));
        return new ConstantCallSite(Pattern_compile.bindTo(pattern));
    }
}
