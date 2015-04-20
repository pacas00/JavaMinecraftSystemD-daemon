package net.petercashel.jmsDd.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.petercashel.jmsDd.module.ModuleSystem;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ASMTransformer {

	public static byte[] transform(String name, byte[] bytes) {

		ClassNode classNode = new ClassNode();
		ClassReader classReader = new ClassReader(bytes);
		classReader.accept(classNode, 0);

		try {
			try {
				for (int i = 0; i < classNode.visibleAnnotations.size(); i++) {
					AnnotationNode ann = (AnnotationNode) classNode.visibleAnnotations.get(i);
					if (ann.desc.equalsIgnoreCase("Lnet/petercashel/jmsDd/module/core/Module;")) {
						try {
							Map<String, Object> values =  asmList2Map(ann.values);
							ModuleSystem.modulesToLoad.put(values.get("ModuleName").toString().replace("[", "").replace("]", ""),
									classNode.name.replace("/", "."));
						} catch (Exception e) {
							e.printStackTrace();


						}
					}
				}
			} catch (Exception e) {
			}
		} catch (Exception e) {
		}
		return bytes;
	}


	private static Map<String, Object> asmList2Map(List list) {
		Map<String, Object> values = new HashMap();
		for (int i = 0; i < list.size(); i+= 2) {
			String name = (String) list.get(i);
			Object val = list.get(i + 1);
			values.put(name, val);
		}
		return values;
	}
}
