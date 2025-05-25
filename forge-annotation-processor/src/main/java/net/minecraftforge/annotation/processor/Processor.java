/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.annotation.processor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.Elements;

@SupportedAnnotationTypes({
	Processor.GENERIC_EVENT,
	Processor.GENERIC_EVENTS
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class Processor extends AbstractProcessor {
	static final String GENERIC_EVENT = "net.minecraftforge.annotation.GenericEventType";
	static final String GENERIC_EVENTS = "net.minecraftforge.annotation.GenericEventTypes";

	private Elements elementUtils;
	private Map<String, GenericEventClassInfo> allSeen = new HashMap<>();

    public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
		this.elementUtils = processingEnv.getElementUtils();
		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation processor initialized");
    }

	private void error(Element element, String message) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
	}

	@SuppressWarnings("unused")
	private void warn(Element element, String message) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, message, element);
	}

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    	var pending = new HashMap<String, GenericEventClassInfo>();

		for (var annotation : annotations) {
			var annotationName = annotation.getQualifiedName().toString();
			if (GENERIC_EVENT.equals(annotationName)) {
				for (var element : roundEnv.getElementsAnnotatedWith(annotation)) {
					var ann = find(GENERIC_EVENT, element);
					if (ann == null) {
						error(element, "Annotation @GenericEvents not found on element: " + element);
						continue;
					}

					processGenericEvent(pending, ann, roundEnv, (TypeElement)element);
				}
			} else if (GENERIC_EVENTS.equals(annotationName)) {
				var annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
				for (var element : annotatedElements) {
					var ann = find(GENERIC_EVENTS, element);
					var anns = expand(element, ann);
					for (var entry : anns)
						processGenericEvent(pending, entry, roundEnv, (TypeElement)element);
				}
			}
		}

		for (var info : pending.values()) {
			if (info.root)
				buildClasses(info);
		}

		allSeen.putAll(pending);
		writeAttachData(allSeen.values());

		return false;
    }

	private static class GenericEventClassInfo {
		final TypeElement element;
		final boolean root;
		final String name;
		final String suffix;
		boolean declared;
		TypeElement type;
		TypeElement parent;
		Map<String, GenericEventClassInfo> children = new HashMap<>();

		GenericEventClassInfo(TypeElement element, boolean root, String name, String suffix) {
			this.element = element;
			this.root = root;
			this.name = name;
			this.suffix = suffix;
		}
	}

	private GenericEventClassInfo findInfo(Map<String, GenericEventClassInfo> pending, String suffix, TypeElement element) {
		var key = element.getQualifiedName() + " - " + suffix;
		var info = pending.get(key);
		if (info != null)
			return info;

		if (element.getEnclosingElement() instanceof TypeElement enclosingElement) {
			var parent = findInfo(pending, suffix, enclosingElement);

			var simpleName = element.getSimpleName().toString();
			info = new GenericEventClassInfo(element, false, simpleName, suffix);
			parent.children.put(simpleName, info);
		} else {
			info = new GenericEventClassInfo(element, true, element.getQualifiedName().toString() + suffix, suffix);
		}
		pending.put(key, info);
		return info;
	}

    private void processGenericEvent(Map<String, GenericEventClassInfo> pending, AnnotationMirror annotation, RoundEnvironment roundEnv, TypeElement element) {
    	var data = convert(element, annotation);
    	if (data == null)
    		return;

    	var toGen = findInfo(pending, data.name, element);

		var declared = declared(element).stream()
			.map(ann -> convert(null, ann))
			.filter(e -> e != null)
			.toList();

		toGen.declared = declared.contains(data);
		toGen.type = data.type;
		if (!toGen.declared) {
			var superType = ((TypeElement)((DeclaredType)element.getSuperclass()).asElement());
			if (!"java.lang.Object".equals(superType.getQualifiedName().toString()))
	            toGen.parent = superType;
		}
    }

    private void buildClasses(GenericEventClassInfo info) {

		var className = info.name;

	    String packageName = null;
	    int idx = className.lastIndexOf('.');
	    if (idx > 0)
	        packageName = className.substring(0, idx);

	    try {
		    var builderFile = processingEnv.getFiler()
	    		.createSourceFile(className, info.element);

		    var imports = new TreeSet<String>();
		    var string = new StringWriter();
		    var writer = new PrintWriter(string);
		    writeClass(imports, info, writer, "");

		    try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
		        if (packageName != null) {
		            out.print("package ");
		            out.print(packageName);
		            out.println(";");
		            out.println();
		        }

		        for (var imp : imports) {
					out.print("import ");
					out.print(imp);
					out.println(";");
		        }

		        out.println();

		        out.println(string.toString());
		    }
	    } catch (IOException e) {
			error(info.element, "Failed to create event class: " + e.getMessage());
			return;
	    }

	}

    private static String simpleName(String className) {
	    int idx = className.lastIndexOf('.');
	    if (idx != -1)
	        return className.substring(idx + 1);
	    return className;
    }

    private static String simpleName(Set<String> imports, TypeElement element, String suffix) {
    	var names = new ArrayList<String>();

		do {
			var qualified = element.getQualifiedName().toString();
	    	names.add(0, simpleName(qualified));
	    	if (element.getEnclosingElement() instanceof TypeElement enclosingElement)
	    		element = enclosingElement;
	    	else {
	    		if (suffix != null && !suffix.isEmpty()) {
	    			names.add(0, names.remove(0) + suffix);
	    			imports.add(qualified + suffix);
	    		} else {
	    			imports.add(qualified);
	    		}
	    		break;
	    	}
		} while (true);

		return String.join(".", names);
    }

    private static String internalName(TypeElement element, String suffix) {
    	var names = new ArrayList<String>();
    	String pkg = null;


		do {
			var qualified = element.getQualifiedName().toString();
	    	names.add(0, simpleName(qualified));
	    	if (element.getEnclosingElement() instanceof TypeElement enclosingElement)
	    		element = enclosingElement;
	    	else {
	    		if (suffix != null && !suffix.isEmpty())
	    			names.add(0, names.remove(0) + suffix);
	    		pkg = qualified.substring(0, qualified.lastIndexOf('.') + 1);
	    		break;
	    	}
		} while (true);

		return pkg.replace('.', '/') + String.join("$", names);
    }

    private static final String[] TEMPLATE = new String[] {
        "public {static}class {name} extends {parent} {",
        "    public {name}({type} instance) {",
        "        super(instance);",
        "    }",
        "}"
    };

    private static final String[] TEMPLATE_WRAPPER = new String[] {
    	"public {static}class {name} {",
    	"}"
    };

    private void writeClass(Set<String> imports, GenericEventClassInfo info, PrintWriter out, String indent) {
    	try {
			var className = simpleName(info.name);
			var parent = "{parent}";
			if (info.parent != null)
				parent = simpleName(imports, info.parent, info.suffix);
			else if (info.declared)
				parent = simpleName(imports, info.type, null);

			var tokens = Map.of(
				"{static}", indent.length() > 0 ? "static " : "",
				"{name}", className,
				"{parent}", parent,
				"{type}", simpleName(imports, info.element, null)
			);

		    boolean child = indent.length() > 0;
			var template = info.parent == null && !info.declared ? TEMPLATE_WRAPPER : TEMPLATE;
			for (int x = 0; x < template.length - 1; x++) {
				if (child) out.print(indent);
				var line = template[x];
				for (var entry : tokens.entrySet())
					line = line.replace(entry.getKey(), entry.getValue());
				out.println(line);
			}

			for (var childInfo : info.children.values()) {
				out.println();
				writeClass(imports, childInfo, out, indent + "    ");
			}

			if (child) out.print(indent);
			var line = template[template.length - 1];
			for (var entry : tokens.entrySet())
				line = line.replace(entry.getKey(), entry.getValue());
			out.println(line);
    	} catch (Throwable t) {
            error(info.element, "Failed to write event class: " + t.getMessage());
    	}
    }

	private void writeAttachData(Collection<GenericEventClassInfo> infos) {
		try {
			var elements = infos.stream()
				.map(info -> info.element)
				.toArray(Element[]::new);

			var attachFile = processingEnv
				.getFiler()
				.createResource(StandardLocation.CLASS_OUTPUT, "", "generic-event-data.json", elements);

			var types = new TreeMap<String, Map<String, String>>();

			for (var info : infos) {
				var type = internalName(info.type, null);
				var name = internalName(info.element, null);
				var event = internalName(info.element, info.suffix);
				types.computeIfAbsent(type, k -> new TreeMap<>()).put(name, event);
			}

			try (var writer = attachFile.openWriter()) {
				writer.write("{\n");
				boolean first = true;
				for (var entry : types.entrySet()) {
					if (!entry.getValue().isEmpty()) {
						if (!first)
							writer.write(",\n");
						first = false;

						writer.write("  \"" + entry.getKey() + "\": {\n");
						boolean subFirst = true;
						for (var subEntry : entry.getValue().entrySet()) {
							if (!subFirst)
								writer.write(",\n");
							subFirst = false;
							writer.write("    \"" + subEntry.getKey() + "\": \"" + subEntry.getValue() + "\"");
						}

						writer.write("\n  }");
					}
				}
				writer.write("\n}\n");
				writer.flush();
			}
		} catch (IOException e) {
			error(null, "Failed to write attach data: " + e.getMessage());
		}
	}

	private record GenericEventTypeInfo(TypeElement type, String name) { }

	private GenericEventTypeInfo convert(Element element, AnnotationMirror annotation) {
		var values = values(annotation);

    	var type = values.get("type");
		if (type == null) {
			if (element != null)
				error(element, "@GenericEventType type not set");
			return null;
		}

		if (!(type.getValue() instanceof DeclaredType typeValue)) {
			if (element != null)
				error(element, "@GenericEventType type is invalid: " + type);
			return null;
		}

		if (!(typeValue.asElement() instanceof TypeElement typeElement)) {
			if (element != null)
				error(element, "@GenericEventType type is not a TypeElement: " + typeValue.asElement());
			return null;
		}

		var name = values.get("name");
		if (name == null) {
			if (element != null)
				error(element, "@GenericEventType name not set");
			return null;
		}

		if (!(name.getValue() instanceof String)) {
			if (element != null)
				error(element, "@GenericEventType name is not a String: " + name);
			return null;
		}

		return new GenericEventTypeInfo(typeElement, name.getValue().toString());
	}

	private Collection<AnnotationMirror> expand(Element element, AnnotationMirror ann) {
		if (ann == null)
			return Collections.emptyList();

		var name = ann.getAnnotationType().toString();
		if (GENERIC_EVENT.equals(name))
			return Collections.singleton(ann);

		var value = values(ann).get("value");
		if (value == null) {
			error(element, "Annotation @GenericEvents value not found on element: " + element);
			return Collections.emptyList();
		}

		@SuppressWarnings("unchecked")
		var values = ((Collection<AnnotationValue>)value.getValue());
		return values.stream().map(e -> (AnnotationMirror)e.getValue()).toList();
	}

	private Collection<AnnotationMirror> declared(Element element) {
		if (element == null)
            return Collections.emptyList();

		for (var mirror : element.getAnnotationMirrors()) {
			var name = mirror.getAnnotationType().toString();
			if (GENERIC_EVENT.equals(name))
				return Collections.singleton(mirror);
			else if (GENERIC_EVENTS.equals(name))
				return expand(element, mirror);
        }

		return Collections.emptyList();
	}

	private AnnotationMirror find(String annotationName, Element element) {
		for (var mirror : this.elementUtils.getAllAnnotationMirrors(element)) {
			if (mirror.getAnnotationType().toString().equals(annotationName))
				return mirror;
		}
		return null;
	}

    private Map<String, AnnotationValue> values(AnnotationMirror mirror) {
        var map = new HashMap<String, AnnotationValue>();
        for (var entry : mirror.getElementValues().entrySet())
            map.put(entry.getKey().getSimpleName().toString(), entry.getValue());
        return map;
    }
}