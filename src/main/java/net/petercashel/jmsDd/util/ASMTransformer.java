/*******************************************************************************
 *    Copyright 2015 Peter Cashel (pacas00@petercashel.net)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *******************************************************************************/

package net.petercashel.jmsDd.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.petercashel.jmsDd.module.ModuleSystem;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ASMTransformer {

	public static byte[] transform(String name, byte[] bytes) {
		final boolean debug = false;
		if (debug) System.out.println(bytes.length);
		ClassNode classNode = new ClassNode();
		String classNameASM = name.replace('.', '/');
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);
		boolean DoModInit = false;
		boolean HasInit = false;
		String initDesc = "()V";
		boolean lockDesc = false;

		try {


			try {
				for (int i = 0; i < classNode.visibleAnnotations.size(); i++) {
					AnnotationNode ann = (AnnotationNode) classNode.visibleAnnotations.get(i);
					if (ann.desc.equalsIgnoreCase("Lnet/petercashel/jmsDd/module/Module;")) {
						try {
							if (debug) System.out.println("ANNOTE!");
							DoModInit = true;
							Map<String, Object> values = asmList2Map(ann.values);
							ModuleSystem.modulesToLoad.put(values.get("ModuleName").toString().replace("[", "")
									.replace("]", ""), classNode.name.replace("/", "."));
						}
						catch (Exception e) {
							e.printStackTrace();

						}
					}
				}
			}
			catch (Exception e) {
				e.printStackTrace();
			}

			try {
				if (DoModInit) {

					for (int i = 0; i < classNode.methods.size(); i++) {
						MethodNode m = (MethodNode) classNode.methods.get(i);
						if (m.name.contentEquals("<init>")) {
							initDesc = m.desc;
							if (m.desc.contentEquals("()V")) {
								HasInit = true;
								if (debug) System.out.println("Found <init>");
							}
						}
					}
				}

			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		if (debug) System.out.println("Still alive?");
		try {

			//			 L0
			//			    LINENUMBER 43 L0
			//			    ALOAD 0
			//			    INVOKESPECIAL // CLASSNAME FOR ASM  // ()V    ////// This is effectically a super() call but to the discovered constructor
			//			   L1
			//			    LINENUMBER 44 L1
			//			    INVOKESTATIC net/petercashel/jmsDd/API/API$Impl.getAPI ()Lnet/petercashel/jmsDd/API/API;
			//			    ALOAD 0
			//			    INVOKEINTERFACE net/petercashel/jmsDd/API/API.registerEventBus (Ljava/lang/Object;)V
			//			   L2
			//			    LINENUMBER 46 L2
			//			    RETURN
			//			   L3
			//			    LOCALVARIABLE this // "L" + CLASSNAME FOR ASM + ";" // L0 L3 0
			//			    LOCALVARIABLE e Lnet/petercashel/jmsDd/event/module/DummyEvent; L0 L3 1
			//			    MAXSTACK = 2
			//			    MAXLOCALS = 2
			if (DoModInit) {
				if (HasInit) {
					if (debug) System.out.println("Adding Extra Constructor to " + name);
					MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(Lnet/petercashel/jmsDd/event/module/DummyEvent;)V", null, null);

					Label L0 = new Label();
					constructor.visitLabel(L0);
					constructor.visitVarInsn(Opcodes.ALOAD, 0);
					constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, classNameASM, "<init>", initDesc);

					Label L1 = new Label();
					constructor.visitLabel(L1);
					constructor.visitMethodInsn(Opcodes.INVOKESTATIC, "net/petercashel/jmsDd/API/API$Impl", "getAPI", "()Lnet/petercashel/jmsDd/API/API;");
					constructor.visitVarInsn(Opcodes.ALOAD, 0);
					constructor.visitMethodInsn(Opcodes.INVOKEINTERFACE, "net/petercashel/jmsDd/API/API", "registerEventBus", "(Ljava/lang/Object;)V");

					Label L2 = new Label();
					constructor.visitLabel(L2);
					constructor.visitInsn(Opcodes.RETURN);

					Label L3 = new Label();
					constructor.visitLabel(L3);
					constructor.visitLocalVariable("this", "L" + classNameASM + ";", null, L0, L3, 0);
					constructor.visitLocalVariable("e", "Lnet/petercashel/jmsDd/event/module/DummyEvent;", null, L0, L3, 1);
					constructor.visitMaxs(2, 2);
					constructor.visitEnd();
					classNode.methods.add(constructor);

				} else { 
					System.err.println("WARNING! " + name + " Doesn't have a default no-args constructor.  Module loader cannot chain load the constructor. \n If you are recieving this error and your module has no constructors defined, or a no-args constructor defined, \n please report the bug to the author of JMSDd.");
					MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "(Lnet/petercashel/jmsDd/event/module/DummyEvent;)V", null, null);

					Label L0 = new Label();
					constructor.visitLabel(L0);
					constructor.visitVarInsn(Opcodes.ALOAD, 0);
					//INVOKESPECIAL java/lang/Object.<init> ()V ////// There is no other constructor, call super() to Object.
					constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

					Label L1 = new Label();
					constructor.visitLabel(L1);
					constructor.visitMethodInsn(Opcodes.INVOKESTATIC, "net/petercashel/jmsDd/API/API$Impl", "getAPI", "()Lnet/petercashel/jmsDd/API/API;");
					constructor.visitVarInsn(Opcodes.ALOAD, 0);
					constructor.visitMethodInsn(Opcodes.INVOKEINTERFACE, "net/petercashel/jmsDd/API/API", "registerEventBus", "(Ljava/lang/Object;)V");

					Label L2 = new Label();
					constructor.visitLabel(L2);
					constructor.visitInsn(Opcodes.RETURN);

					Label L3 = new Label();
					constructor.visitLabel(L3);
					constructor.visitLocalVariable("this", "L" + classNameASM + ";", null, L0, L3, 0);
					constructor.visitLocalVariable("e", "Lnet/petercashel/jmsDd/event/module/DummyEvent;", null, L0, L3, 1);
					constructor.visitMaxs(2, 2);
					constructor.visitEnd();
					classNode.methods.add(constructor);
				}
				classNode.visitEnd();
				ClassWriter wr = new ClassWriter(0);
				classNode.accept(wr);

				bytes = wr.toByteArray();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (debug) System.out.println("Still alive.");
		if (debug) System.out.println(bytes.length);
		return bytes;
	}

	private static Map<String, Object> asmList2Map(List list) {
		Map<String, Object> values = new HashMap();
		for (int i = 0; i < list.size(); i += 2) {
			String name = (String) list.get(i);
			Object val = list.get(i + 1);
			values.put(name, val);
		}
		return values;
	}
}
