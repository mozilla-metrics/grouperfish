package com.mozilla.grouperfish.input;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

import com.mozilla.grouperfish.base.StreamTool;
import com.mozilla.grouperfish.json.TsvJsonWriter;
import com.mozilla.grouperfish.model.Document;

public class TsvJsonFromInputTsv {

    public static void main(String[] args) throws IOException {

        TsvJsonWriter writer =
            new TsvJsonWriter(
                new BufferedWriter(
                        new OutputStreamWriter(System.out, StreamTool.UTF8)));


        for (final Document doc : new OpinionStream(System.in)) {
            writer.write(doc);
        };

        writer.flush();
    }

}
