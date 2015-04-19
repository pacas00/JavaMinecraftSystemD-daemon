package net.petercashel.jmsDd.module.core;


import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Module {
	String ModuleName();
}
