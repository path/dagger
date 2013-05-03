/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dagger.internal.codegen;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Qualifier;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

/**
 * Support for code generation using javac's mirror APIs. Unlike {@code Keys},
 * this class uses APIs not available on Android.
 *
 * <h3>Errors</h3>
 * Upon encountering an error this class may report errors directly to the
 * processor. In such cases a reasonable default value will be returned.
 * Regardless the reported error should cause code analysis to fail.
 */
public abstract class DaggerProcessor extends AbstractProcessor {
  private static final String SET_PREFIX = Set.class.getCanonicalName() + "<";

  public ProcessingEnvironment getEnv() {
    return processingEnv;
  }

  public void error(String msg, Element element) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element);
  }

  /**
   * Returns the members injector key for the raw type of {@code type}.
   * Parameterized types are not currently supported for members injection in
   * generated code.
   */
  public String rawMembersKey(TypeMirror type, Element context) {
    try {
      return "members/" + CodeGen.rawTypeToString(type, '$');
    } catch (IllegalArgumentException unexpectedType) {
      error("Cannot inject " + context + ": " + unexpectedType.getMessage(), context);
      return "members/java.lang.Object";
    }
  }

  public String rawMembersKey(TypeElement type) {
    return rawMembersKey(type.asType(), type);
  }

  /** Returns the provider key for {@code type}. */
  public String key(TypeMirror type, Element context) {
    try {
      StringBuilder result = new StringBuilder();
      CodeGen.typeToString(type, result, '$');
      return result.toString();
    } catch (IllegalArgumentException unexpectedType) {
      error("Cannot inject " + context + ": " + unexpectedType.getMessage(), context);
      return "java.lang.Object";
    }
  }

  public String typeToString(TypeMirror typeMirror) {
    try {
      return CodeGen.typeToString(typeMirror);
    } catch (IllegalArgumentException unexpectedType) {
      return "java.lang.Object"; // Errors will be reported elsewhere.
    }
  }

  public String key(TypeElement type) {
    return key(type.asType(), type);
  }

  /** Returns the provider key for the raw type of {@code type}. */
  public String rawKey(TypeMirror type, Element context) {
    try {
      return CodeGen.rawTypeToString(type, '$');
    } catch (IllegalArgumentException unexpectedType) {
      error("Cannot inject " + context + ": " + unexpectedType.getMessage(), context);
      return "java.lang.Object";
    }
  }

  public String rawKey(TypeElement type) {
    return rawKey(type.asType(), type);
  }

  /** Returns the provided key for {@code method}. */
  public String key(ExecutableElement method) {
    try {
      StringBuilder result = new StringBuilder();
      AnnotationMirror qualifier = getQualifier(method.getAnnotationMirrors(), method);
      if (qualifier != null) {
        qualifierToString(qualifier, result);
      }
      CodeGen.typeToString(method.getReturnType(), result, '$');
      return result.toString();
    } catch (IllegalArgumentException unexpectedType) {
      error("Cannot inject " + method + ": " + unexpectedType.getMessage(), method);
      return "java.lang.Object";
    }
  }

  /** Returns the provided key for {@code method} wrapped by {@code Set}. */
  public String setKey(ExecutableElement method) {
    try {
      StringBuilder result = new StringBuilder();
      AnnotationMirror qualifier = getQualifier(method.getAnnotationMirrors(), method);
      if (qualifier != null) {
        qualifierToString(qualifier, result);
      }
      result.append(SET_PREFIX);
      CodeGen.typeToString(method.getReturnType(), result, '$');
      result.append(">");
      return result.toString();
    } catch (IllegalArgumentException unexpectedType) {
      error("Cannot inject " + method + ": " + unexpectedType.getMessage(), method);
      return "java.lang.Object";
    }
  }

  /** Returns the provider key for {@code variable}. */
  public String key(VariableElement variable) {
    try {
      StringBuilder result = new StringBuilder();
      AnnotationMirror qualifier = getQualifier(variable.getAnnotationMirrors(), variable);
      if (qualifier != null) {
        qualifierToString(qualifier, result);
      }
      CodeGen.typeToString(variable.asType(), result, '$');
      return result.toString();
    } catch (IllegalArgumentException unexpectedType) {
      error("Cannot inject " + variable + ": " + unexpectedType.getMessage(), variable);
      return "java.lang.Object";
    }
  }

  private static void qualifierToString(AnnotationMirror qualifier, StringBuilder result) {
    // TODO: guarantee that element values are sorted by name (if there are multiple)
    result.append('@');
    CodeGen.typeToString(qualifier.getAnnotationType(), result, '$');
    result.append('(');
    for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry
        : qualifier.getElementValues().entrySet()) {
      result.append(entry.getKey().getSimpleName());
      result.append('=');
      result.append(entry.getValue().getValue());
    }
    result.append(")/");
  }

  private static AnnotationMirror getQualifier(
      List<? extends AnnotationMirror> annotations, Object member) {
    AnnotationMirror qualifier = null;
    for (AnnotationMirror annotation : annotations) {
      if (annotation.getAnnotationType().asElement().getAnnotation(Qualifier.class) == null) {
        continue;
      }
      if (qualifier != null) {
        throw new IllegalArgumentException("Too many qualifier annotations on " + member);
      }
      qualifier = annotation;
    }
    return qualifier;
  }
}
