/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *  
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Please contact Codename One through http://www.codenameone.com/ if you 
 * need additional information or have any questions.
 */

package com.codename1.tools.javadoc.sourceembed.javadocsourceembed;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.json.JSONObject;

/**
 * <p>A trivial tool that allows embedding Java source code into JavaDoc using github gists in a way that
 * works nicely in the browser and in the ide...</p>
 * <p>To use this just use the standard gist embed code within your javadoc comments then run this tool by
 * supplying the source directory and a destination directory to which the modified sources should be written
 * then run javadoc on the modified code. Notice that you shouldn't modify the standrad code as the generated
 * code is a bit too "loud".
 * </p>
 *
 * @author Shai Almog
 */
public class Main {
    private static final String CLIENT_ID = "";
    private static final String CLIENT_SECRET = "";
    private static Charset CHARSET;
    private static final HashMap<String, String> gistCache = new HashMap<>();
    public static void main(String[] args) throws Exception {
        // this accepts two arguments source directory and destination directory where the modfied files will
        // be written
        File sourceDir = new File(args[0]);
        File destDir = new File(args[1]);
        System.out.println("JavaDoc conversion " + sourceDir.getAbsolutePath() + " to " + destDir.getAbsolutePath());
        if(!sourceDir.exists() || !sourceDir.isDirectory()) {
            System.out.println("Source directory doesn't exist");
            System.exit(1);
            return;
        }
        CHARSET = Charset.forName("UTF-8");
        directoryWalker(sourceDir, destDir);
    }
    
    public static void directoryWalker(File f, File destDir) throws Exception {
        File[] files = f.listFiles((path) -> path.isDirectory() || path.getName().endsWith(".java"));
        
        for(File ff : files) {
            if(ff.isDirectory()) {
                File destDirT = new File(destDir, ff.getName());
                destDirT.mkdirs();
                directoryWalker(ff, destDirT);
            } else {
                processFile(ff, new File(destDir, ff.getName()));
            }
        }
    }
    
    public static void processFile(File javaSourceFile, File javaDestFile) throws Exception {
        System.out.println("JavaSource Processing: " + javaSourceFile.getName());
        List<String> lines = Files.readAllLines(Paths.get(javaSourceFile.toURI()), CHARSET);
        for(int iter  = 0 ; iter < lines.size() ; iter++) {
            String l = lines.get(iter);
            int position = l.indexOf("<script src=\"https://gist.github.com/");
            if(position > -1) {
                String id = l.substring(position + 39 );
                id = id.split("/")[1];
                id = id.substring(0, id.indexOf('.'));
                String fileContent = gistCache.get(id);
                if(fileContent != null) {
                    lines.add(iter + 1, fileContent);
                    iter++;
                    continue;
                }
                
                URL u = new URL("https://api.github.com/gists/" + id + "?client_id=" + CLIENT_ID + "&client_secret=" + CLIENT_SECRET);
                try(BufferedReader br = new BufferedReader(new InputStreamReader(u.openStream(), CHARSET))) {
                    String jsonText = br.lines().collect(Collectors.joining("\n"));
                    JSONObject json = new JSONObject(jsonText);
                    JSONObject filesObj = json.getJSONObject("files");
                    String str ="";
                    for(String k : filesObj.keySet()) {
                        JSONObject jsonFileEntry = filesObj.getJSONObject(k);
                        // breaking line to fix the problem with a blank space on the first line
                        String current  = "\n" + jsonFileEntry.getString("content");
                        str += current;
                    }
                    int commentStartPos = str.indexOf("/*");
                    while(commentStartPos > -1) {
                        // we just remove the comment as its pretty hard to escape it properly
                        int commentEndPos = str.indexOf("*/");
                        str = str.substring(commentStartPos, commentEndPos + 1);
                        commentStartPos = str.indexOf("/*");
                    }

                    // allows things like the @Override annotation
                    str = "<noscript><pre>{@code " + str.replace("@", "{@literal @}") + "}</pre></noscript>"; 
                    gistCache.put(id, str);
                    lines.add(iter + 1, str);
                    iter++;
                }
            }
        }
        try(BufferedWriter fw = new BufferedWriter(new FileWriter(javaDestFile))) {
            for(String l : lines) {
                fw.write(l);
                fw.write('\n');
            }
        }
    }
}
