/*
 * Copyright (C) 2011 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.juzu.impl.compiler;

import org.juzu.impl.compiler.file.FileKey;
import org.juzu.impl.compiler.file.SimpleFileManager;
import org.juzu.impl.compiler.file.JavaFileObjectImpl;
import org.juzu.impl.fs.Visitor;
import org.juzu.impl.spi.fs.ReadFileSystem;
import org.juzu.impl.spi.fs.ReadWriteFileSystem;
import org.juzu.impl.utils.Spliterator;
import org.juzu.text.Location;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
public class Compiler
{

   public static class Builder
   {

      /** . */
      private final ReadFileSystem<?> sourcePath;

      /** . */
      private final ReadWriteFileSystem<?> classOutput;

      /** . */
      private final ReadWriteFileSystem<?> sourceOutput;

      /** . */
      private final List<ReadFileSystem<?>> classPaths;

      private Builder(
         ReadFileSystem<?> sourcePath,
         ReadWriteFileSystem<?> sourceOutput,
         ReadWriteFileSystem<?> classOutput,
         List<ReadFileSystem<?>> classPaths)
      {
         this.sourcePath = sourcePath;
         this.sourceOutput = sourceOutput;
         this.classOutput = classOutput;
         this.classPaths = classPaths;
      }

      public Builder classOutput(ReadWriteFileSystem<?> classOutput)
      {
         return new Builder(sourcePath, sourceOutput, classOutput, classPaths);
      }

      public Builder output(ReadWriteFileSystem<?> output)
      {
         return new Builder(sourcePath, output, output, classPaths);
      }

      public Builder sourcePath(ReadFileSystem<?> sourcePath)
      {
         return new Builder(sourcePath, sourceOutput, classOutput, classPaths);
      }

      public Builder sourceOutput(ReadWriteFileSystem<?> sourceOutput)
      {
         return new Builder(sourcePath, sourceOutput, classOutput, classPaths);
      }

      public Builder addClassPath(ReadFileSystem<?> classPath)
      {
         classPaths.add(classPath);
         return this;
      }

      public Compiler build()
      {
         if (sourcePath == null)
         {
            throw new IllegalStateException("No null source path");
         }
         if (classOutput == null)
         {
            throw new IllegalStateException("No null class output");
         }
         if (sourceOutput == null)
         {
            throw new IllegalStateException("No null source output");
         }
         return new Compiler(
            sourcePath,
            classPaths,
            sourceOutput,
            classOutput
         );
      }

      public static Builder create()
      {
         return new Builder(null, null, null, new ArrayList<ReadFileSystem<?>>());
      }
   }

   /** . */
   static final Pattern PATTERN = Pattern.compile("\\[" + "([^\\]]+)" + "\\]\\(" + "([^\\)]*)" + "\\)");

   /** . */
   private JavaCompiler compiler;

   /** . */
   private VirtualFileManager fileManager;

   /** . */
   private Set<Processor> processors;

   public Compiler(
      ReadFileSystem<?> sourcePath,
      ReadWriteFileSystem<?> output)
   {
      this(sourcePath, output, output);
   }

   public Compiler(
      ReadFileSystem<?> sourcePath,
      ReadWriteFileSystem<?> sourceOutput,
      ReadWriteFileSystem<?> classOutput)
   {
      this(sourcePath, Collections.<ReadFileSystem<?>>emptyList(), sourceOutput, classOutput);
   }

   public Compiler(
      ReadFileSystem<?> sourcePath,
      ReadFileSystem<?> classPath,
      ReadWriteFileSystem<?> sourceOutput,
      ReadWriteFileSystem<?> classOutput)
   {
      this(sourcePath, Collections.<ReadFileSystem<?>>singletonList(classPath), sourceOutput, classOutput);
   }

   public Compiler(
      ReadFileSystem<?> sourcePath,
      Collection<ReadFileSystem<?>> classPath,
      ReadWriteFileSystem<?> sourceOutput,
      ReadWriteFileSystem<?> classOutput)
   {
      this.compiler = ToolProvider.getSystemJavaCompiler();
      this.fileManager = new VirtualFileManager(
         compiler.getStandardFileManager(null, null, null),
         sourcePath,
         classPath,
         sourceOutput,
         classOutput
      );
      this.processors = new HashSet<Processor>();
   }

   public void addAnnotationProcessor(Processor annotationProcessorType)
   {
      if (annotationProcessorType == null)
      {
         throw new NullPointerException("No null processor allowed");
      }
      processors.add(annotationProcessorType);
   }

   public ReadWriteFileSystem<Object> getSourceOutput()
   {
      return (ReadWriteFileSystem<Object>)fileManager.sourceOutput.getFileSystem();
   }

   public ReadWriteFileSystem<Object> getClassOutput()
   {
      return (ReadWriteFileSystem<Object>)fileManager.classOutput.getFileSystem();
   }

   public List<CompilationError> compile(String... compilationUnits) throws IOException
   {
      return compile(getFromSourcePath(fileManager.sourcePath, compilationUnits));
   }

   public List<CompilationError> compile() throws IOException
   {
      return compile(getFromSourcePath(fileManager.sourcePath));
   }

   private <P> Iterable<JavaFileObject> getFromSourcePath(SimpleFileManager<P> manager, String... compilationUnits) throws IOException
   {
      ArrayList<String> tmp = new ArrayList<String>();
      final ArrayList<JavaFileObject> javaFiles = new ArrayList<JavaFileObject>();
      for (String compilationUnit : compilationUnits)
      {
         ArrayList<String> names = Spliterator.split(compilationUnit, '/', tmp);
         String name = tmp.get(tmp.size() - 1);
         if (!name.endsWith(".java"))
         {
            throw new IllegalArgumentException("Illegal compilation unit: " + compilationUnit);
         }
         P file = manager.getFileSystem().getPath(names);
         if (file == null)
         {
            throw new IllegalArgumentException("Could not find compilation unit: " + compilationUnit);
         }
         StringBuilder sb = new StringBuilder();
         manager.getFileSystem().packageOf(file, '.', sb);
         FileKey key = FileKey.newJavaName(sb.toString(), name.substring(0, name.length()));
         javaFiles.add(manager.getReadable(key));
      }
      return javaFiles;
   }

   private <P> Iterable<JavaFileObject> getFromSourcePath(final SimpleFileManager<P> fileManager) throws IOException
   {
      final ArrayList<JavaFileObject> javaFiles = new ArrayList<JavaFileObject>();
      fileManager.getFileSystem().traverse(new Visitor.Default<P>()
      {
         public void file(P file, String name) throws IOException
         {
            if (name.endsWith(".java"))
            {
               StringBuilder sb = new StringBuilder();
               fileManager.getFileSystem().packageOf(file, '.', sb);
               FileKey key = FileKey.newJavaName(sb.toString(), name);
               javaFiles.add(fileManager.getReadable(key));
            }
         }
      });
      return javaFiles;
   }

   private List<CompilationError> compile(Iterable<JavaFileObject> compilationUnits) throws IOException
   {
      final List<CompilationError> errors = new ArrayList<CompilationError>();
      DiagnosticListener<JavaFileObject> listener = new DiagnosticListener<JavaFileObject>()
      {
         public void report(Diagnostic<? extends JavaFileObject> diagnostic)
         {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR)
            {
               int columnNumber = (int)diagnostic.getColumnNumber();
               int lineNumber = (int)diagnostic.getLineNumber();
               Location location = (columnNumber > 0 && lineNumber > 0) ? new Location(columnNumber, lineNumber) : null;

               //
               String message = diagnostic.getMessage(null);

               //
               String code = null;
               List<String> arguments = Collections.emptyList();
               Matcher matcher = PATTERN.matcher(message);
               if (matcher.find())
               {
                  code = matcher.group(1);
                  if (matcher.group(2).length() > 0)
                  {
                     arguments = new ArrayList<String>();
                     for (String argument : Spliterator.split(matcher.group(2), ','))
                     {
                        arguments.add(argument.trim());
                     }
                  }
               }

               //
               JavaFileObject obj = diagnostic.getSource();
               String source = null;
               File resolvedFile = null;
               if (obj != null)
               {
                  source = obj.getName().toString();
                  if (obj instanceof JavaFileObjectImpl)
                  {
                     JavaFileObjectImpl foo = (JavaFileObjectImpl)obj;
                     try
                     {
                        resolvedFile = foo.getFile();
                     }
                     catch (Exception e)
                     {
                     }

                  }
               }

               //
               errors.add(new CompilationError(code, arguments, source, resolvedFile, location, message));
            }
         }
      };

      //
      JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, listener, Collections.<String>emptyList(), null, compilationUnits);
      task.setProcessors(processors);

      // We don't use the return value because sometime it says it is failed although
      // it is not, need to investigate this at some piont
      boolean ok = task.call();

      // Clear caches
      fileManager.sourceOutput.clearCache();
      fileManager.classOutput.clearCache();
      fileManager.sourceOutput.clearCache();
      if (fileManager.classPath != null)
      {
         fileManager.classPath.clearCache();
      }

      // Clear processors as we should not reuse them
      processors.clear();

      //
      return ok ? Collections.<CompilationError>emptyList() : errors;
   }
}
