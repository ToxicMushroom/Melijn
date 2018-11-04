package me.melijn.jda.commands.developer;

import gnu.trove.list.TLongList;
import gnu.trove.list.array.TLongArrayList;
import me.melijn.jda.blub.Category;
import me.melijn.jda.blub.Command;
import me.melijn.jda.blub.CommandEvent;
import me.melijn.jda.utils.MessageHelper;
import me.melijn.jda.utils.TaskScheduler;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.ISimpleCompiler;

import java.lang.reflect.Method;

import static me.melijn.jda.Melijn.PREFIX;

public class EvalCommand extends Command {

    public static TLongList serverBlackList = new TLongArrayList();
    public static TLongList userBlackList = new TLongArrayList();
    private static final String CLASS_NAME = "EvalTempClass";

    public EvalCommand() {
        this.commandName = "eval";
        this.description = "eval stuff";
        this.usage = PREFIX + commandName + " [insert crappy code]";
        this.category = Category.DEVELOPER;
    }

    @Override
    protected void execute(CommandEvent event) {
        TaskScheduler.async(() -> {
            try {
                evaluate(event.getArgs(), event);
            } catch (Exception e) {
                MessageHelper.sendSplitMessage(event.getTextChannel(), "```" + e.getMessage() + "```");
            }
        });
    }

    private static void evaluate(final String source, CommandEvent event) throws Exception {
        final ISimpleCompiler compiler = CompilerFactoryFactory.getDefaultCompilerFactory().newSimpleCompiler();
        compiler.cook(createDummyClassSource(source));
        evaluateDummyClassMethod(event, compiler.getClassLoader());
    }

    private static String createDummyClassSource(final String source) {
        return  "import me.melijn.jda.blub.*;\n" +
                "import me.melijn.jda.utils.*;\n" +
                "import me.melijn.jda.Melijn;\n" +
                "import me.melijn.jda.commands.*;\n" +
                "import me.melijn.jda.commands.management.*;\n" +
                "import me.melijn.jda.commands.util.*;\n" +
                "import me.melijn.jda.commands.music.*;\n" +
                "import me.melijn.jda.commands.fun.*;\n" +
                "import me.melijn.jda.commands.developer.*;\n" +
                "import me.melijn.jda.Helpers;\n" +
                "import me.melijn.jda.*;\n" +
                "import java.io.*;\n" +
                "import java.lang.*;\n" +
                "import java.util.*;\n" +
                "import java.util.concurrent.*;\n" +
                "import net.dv8tion.jda.core.*;\n" +
                "import net.dv8tion.jda.core.entities.*;\n" +
                "import net.dv8tion.jda.core.entities.impl.*;\n" +
                "import net.dv8tion.jda.core.managers.*;\n" +
                "import net.dv8tion.jda.core.managers.impl.*;\n" +
                "import net.dv8tion.jda.core.utils.*;\n" +
                "import java.util.regex.*;\n" +
                "import java.awt.*;\n" +
                "class " + CLASS_NAME + " {\n" +
                "   public static void eval(final CommandEvent event) {\n" +
                "       " + source + "\n" +
                "   }\n" +
                "}\n";
    }

    private static void evaluateDummyClassMethod(final CommandEvent event, final ClassLoader classLoader) throws Exception {
        final Class<?> dummy = classLoader.loadClass(CLASS_NAME);
        final Method eval = dummy.getDeclaredMethod("eval", CommandEvent.class);
        eval.setAccessible(true);
        eval.invoke(null, event);
    }
}
