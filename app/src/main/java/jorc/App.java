/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package jorc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Scanner;
import java.util.ArrayList;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;

import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;

public class App {
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        Option inputOption = new Option("i", "input", true, "input file");

        options.addOption("h", "help", false, "");
        options.addOption("f", "force", false, "do not prompt before overwriting an existing output file");
        options.addOption("o", "output", true, "output file");
        options.addOption(inputOption);

        HelpFormatter formatter = new HelpFormatter();

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
            formatter.printHelp("myapp", "Rebuild the local variable tables in a class file", options, "", true);
            return;
        }
        if (!cmd.hasOption(inputOption)) {
            List<Option> missingOptions = new ArrayList<Option>(1);
            missingOptions.add(inputOption);
            throw new MissingOptionException(missingOptions);
        }
        String inputFileName = cmd.getOptionValue(inputOption);
        


        FileInputStream fi = new FileInputStream(inputFileName);
        ClassReader cr = new ClassReader(fi);

        ClassNode cn = new ClassNode();
        
        cr.accept(cn, 0);

        for (MethodNode methodNode : cn.methods) {
            Analyzer<BasicValue> analyzer = new Analyzer<>(new ObjectTypeInterpreter());
            analyzer.analyze(cn.name, methodNode);

            System.out.println(String.format("Examining method %s", methodNode.name));
            Frame<BasicValue>[] frames = analyzer.getFrames();
            AbstractInsnNode[] instructions=  methodNode.instructions.toArray();

            if (methodNode.localVariables != null && methodNode.localVariables.size() > 0) {
                System.err.println(String.format("Method %s already contains local variables, skipping...", methodNode.name));
                continue;
            }
            Type methodType = Type.getType(methodNode.desc);
            boolean isMethodStatic = (methodNode.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
            LabelNode last = null;
            if (!isMethodStatic || methodType.getArgumentTypes().length != 0) {
                LabelNode first = new LabelNode();
                last = new LabelNode();

                methodNode.instructions.insert(first);
                methodNode.instructions.add(last);

                int index = 0;
                if (!isMethodStatic) {
                    LocalVariableNode thisVar = new LocalVariableNode("this", Type.getObjectType(cn.name).toString(), null, first, last, index);
                    methodNode.localVariables.add(thisVar);
                    index++;
                }
                Type[] argTypes = methodType.getArgumentTypes();
                for (int i = 0; i < argTypes.length; i++) {
                    Type argType = argTypes[i];
                    LocalVariableNode argVar = new LocalVariableNode(String.format("arg%d", i+1), argType.getDescriptor(), null, first, last, index);
                    methodNode.localVariables.add(argVar);
                    index += argType.getSize();
                }
            }
            for (int i = 0; i < instructions.length; i++) {
                Frame<BasicValue> frame = frames[i];
                
                if (instructions[i].getType() == AbstractInsnNode.VAR_INSN) {
                    int opcode = instructions[i].getOpcode();
                    if (
                        opcode == Opcodes.ASTORE ||
                        opcode == Opcodes.ISTORE ||
                        opcode == Opcodes.DSTORE ||
                        opcode == Opcodes.FSTORE ||
                        opcode == Opcodes.LSTORE
                    ) {
                        VarInsnNode insn = (VarInsnNode)instructions[i];
                        Type varType = frame.getStack(frame.getStackSize() - 1).getType();
                        System.out.println(String.format("Local var index %d: %s", insn.var, varType.toString()));
                    
                        if (last == null) {
                            last = new LabelNode();
                            methodNode.instructions.add(last);
                        }

                        /*
                         * This logic prevents duplication of arg (aka. param) vars,
                         * which are scoped to the entire method
                         */
                        boolean exists = false;
                        for (LocalVariableNode var : methodNode.localVariables) {
                            if (var.index == insn.var && var.end == last) {
                                exists = true;
                            }
                        }
                        if (exists) {
                            continue;
                        }
                        

                        LabelNode varStart = new LabelNode();
                        methodNode.instructions.insert(insn, varStart);
    
                        LocalVariableNode localVar = new LocalVariableNode("local"+insn.var, varType.toString(), null, varStart, last, insn.var);
                        methodNode.localVariables.add(localVar);
                    }
                }
                else if (instructions[i].getType() == AbstractInsnNode.LABEL) {
                    for (LocalVariableNode localVar : methodNode.localVariables) {
                        if (localVar.end == last &&
                            (localVar.index >= frame.getLocals() ||
                             frame.getLocal(localVar.index).getType() == null)
                            ) {
                            localVar.end = (LabelNode)instructions[i];
                        }
                    }
                }
            }
        }
        // No need to recompute max or frames
        // because we aren't actually changing
        // the methods
        ClassWriter classWriter = new ClassWriter(0);
        cn.accept(classWriter);
 
        String outputFileName;
        if (!cmd.hasOption("o")) {
            outputFileName = inputFileName;
        }
        else {
            outputFileName = cmd.getOptionValue("o");
        }

        File outputFile = new File(outputFileName);
        if (outputFile.exists() && !cmd.hasOption("f")) {
            Scanner scanner = new Scanner(System.in);
            System.err.println(String.format("Overwrite the existing file %s?", outputFileName));
            if (!scanner.nextBoolean()) {
                scanner.close();
                return;
            }
            scanner.close();

        }

        OutputStream dout = new FileOutputStream(outputFile);
        dout.write(classWriter.toByteArray());
        dout.flush();
        dout.close();
    }
}
