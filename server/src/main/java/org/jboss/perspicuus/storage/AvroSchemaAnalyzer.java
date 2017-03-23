/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.perspicuus.storage;

import org.apache.avro.Schema;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.util.StopwordAnalyzerBase;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Analyzer to help make Avro's Schema structure indexable by Hibernate Search i.e. lucene.
 *
 * Currently unused, but may be a more flexible approach than the exiting AvroSchemaStringBridge.
 *
 * @since 2017-03
 * @author Jonathan Halliday (jonathan.halliday@redhat.com)
 */
public class AvroSchemaAnalyzer extends StopwordAnalyzerBase {

    // should probably be a subclass of StandardAnalyzer, but that's final :-(


    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {

        final Tokenizer src = new StandardTokenizer();

        TokenStream tok = new StandardFilter(src);
        tok = new LowerCaseFilter(tok);
        tok = new StopFilter(tok, stopwords);

        return new TokenStreamComponents(src, tok) {
            @Override
            protected void setReader(final Reader reader) throws IOException {

                char[] array = new char[4 * 1024];
                StringBuilder buffer = new StringBuilder();
                int count;
                while ((count = reader.read(array, 0, array.length)) != -1) {
                    buffer.append(array, 0, count);
                }

                String data = buffer.toString();

                if(data.startsWith("{")) {

                    Schema avroSchema = new Schema.Parser().parse(data);
                    List<String> tokens = new ArrayList<>();

                    tokens.add(avroSchema.getName());

                    for (Schema.Field field : avroSchema.getFields()) {
                        tokens.add(field.name());
                    }

                    String result = String.join(" ", tokens);
                    data = result;
                }

                super.setReader( new StringReader(data) );
            }
        };
    }
}
