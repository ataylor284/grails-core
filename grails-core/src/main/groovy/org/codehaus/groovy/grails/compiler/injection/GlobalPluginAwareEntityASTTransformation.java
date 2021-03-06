/* Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.compiler.injection;

import grails.util.BuildSettingsHolder;
import grails.util.PluginBuildSettings;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.grails.plugins.GrailsPluginInfo;
import org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

/**
 * Automatically annotates any class with @Plugin(name="foo") if it is a plugin resource.
 *
 * @author Graeme Rocher
 * @since 1.2
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class GlobalPluginAwareEntityASTTransformation implements ASTTransformation {

    private boolean disableTransformation = Boolean.getBoolean("disable.grails.plugin.transform");
    PluginBuildSettings pluginBuildSettings;

    public GlobalPluginAwareEntityASTTransformation() {
        pluginBuildSettings = new PluginBuildSettings(BuildSettingsHolder.getSettings());
    }

    public void visit(ASTNode[] astNodes, SourceUnit sourceUnit) {
        if (disableTransformation) {
            return;
        }

        ASTNode astNode = astNodes[0];
        if (!(astNode instanceof ModuleNode)) {
            return;
        }

        ModuleNode moduleNode = (ModuleNode) astNode;
        List<?> classes = moduleNode.getClasses();
        if (classes.isEmpty()) {
            return;
        }

        ClassNode classNode = (ClassNode) classes.get(0);
        if (classNode.isAnnotationDefinition()) {
            return;
        }

        File sourcePath = new File(sourceUnit.getName());
        try {
            String absolutePath = sourcePath.getCanonicalPath();
            if (pluginBuildSettings == null) {
                return;
            }

            GrailsPluginInfo info = pluginBuildSettings.getPluginInfoForSource(absolutePath);
            if (info == null) {
                return;
            }

            final ClassNode annotation = new ClassNode(GrailsPlugin.class);
            final List<?> list = classNode.getAnnotations(annotation);
            if (!list.isEmpty()) {
                return;
            }

            final AnnotationNode annotationNode = new AnnotationNode(annotation);
            annotationNode.addMember(org.codehaus.groovy.grails.plugins.GrailsPlugin.NAME,
                    new ConstantExpression(info.getName()));
            annotationNode.addMember(org.codehaus.groovy.grails.plugins.GrailsPlugin.VERSION,
                    new ConstantExpression(info.getVersion()));
            annotationNode.setRuntimeRetention(true);
            annotationNode.setClassRetention(true);

            classNode.addAnnotation(annotationNode);
        }
        catch (IOException e) {
            // ignore
        }
    }
}
