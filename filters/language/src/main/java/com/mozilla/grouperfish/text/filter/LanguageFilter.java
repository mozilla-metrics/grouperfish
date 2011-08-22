/*
 * Copyright 2011 Mozilla Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mozilla.grouperfish.text.filter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.regex.Pattern;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;

public class LanguageFilter {

    public LanguageFilter(String profileDirectory) throws LangDetectException {
        DetectorFactory.loadProfile(profileDirectory);
    }
    
    // isLanguage("The quick brown fox.", "en")
    public boolean isLanguage(String text, String langAbbr) {
        Detector detector;
        boolean isLang = false;
        try {
            detector = DetectorFactory.create();
            detector.append(text);
            isLang = detector.detect().startsWith(langAbbr);
        } catch (LangDetectException e) {
            //System.out.println("Could not detect language on: " + text);
        }
        
        
        return isLang;
    }
    
    public static void main(String[] args) throws IOException {
        String inputPath = args[0];
        String profilesPath = args[1];
        String desiredLang = args[2];
        String outputPath = args[3];
        
        BufferedReader reader = null;
        BufferedWriter writer = null;
        try {
            LanguageFilter langFilter = new LanguageFilter(profilesPath);
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(inputPath), "UTF-8"));
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath), "UTF-8"));
            String line = null;
            Pattern tabPattern = Pattern.compile("\t");
            long total = 0;
            long passed = 0;
            while ((line = reader.readLine()) != null) {
                total++;
                String[] fields = tabPattern.split(line);
                
                // Skip if we have more tabs (work around this later)
                if (fields.length != 8) continue;
                
                // Filter lang. detection
                // !fields[6].toLowerCase().startsWith(desiredLang) && 
                if (!langFilter.isLanguage(fields[7], desiredLang)) continue;

                // Write original line back to output
                writer.write(line);
                writer.write('\n');
                passed++;
                
                if (total % 1000 == 0) {
                    System.out.printf("Processed %d lines\n", total);
                }
            }
            
            System.out.printf("Percentage that passed: %.2f%%\n", ((float)passed/(float)total)*100.0f);
        } catch (LangDetectException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                 // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
    
}
