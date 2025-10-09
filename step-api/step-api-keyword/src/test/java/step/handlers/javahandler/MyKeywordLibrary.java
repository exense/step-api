/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.handlers.javahandler;

import step.core.reports.Measure;
import step.reporting.LiveReporting;
import step.reporting.impl.LiveMeasureSink;
import step.streaming.client.upload.StreamingUpload;
import step.streaming.client.upload.impl.local.DiscardingStreamingUploadProvider;
import step.streaming.client.upload.impl.local.LocalDirectoryBackedStreamingUploadProvider;
import step.streaming.common.QuotaExceededException;
import step.streaming.common.StreamingResourceStatus;
import step.streaming.common.StreamingResourceTransferStatus;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class MyKeywordLibrary extends AbstractKeyword {

	public static final String ON_ERROR_MARKER = "onError";
	public static final String THROW_EXCEPTION_IN_AFTER = "throwExceptionInAfter";
	public static final String THROW_EXCEPTION_IN_BEFORE = "throwExceptionInBefore";
	public static final String RETHROW_EXCEPTION_IN_ON_ERROR = "rethrowException";

	private String keywordClassProperty;

	public String getKeywordClassProperty() {
		return keywordClassProperty;
	}

	public void setKeywordClassProperty(String keywordClassProperty) {
		this.keywordClassProperty = keywordClassProperty;
	}

	@Override
	public void beforeKeyword(String keywordName,Keyword keyword) {
		if (!keywordName.contains("Return")) {
			output.add("beforeKeyword", keywordName);
		}
		if(getBooleanProperty(THROW_EXCEPTION_IN_BEFORE)) {
			throw new RuntimeException(THROW_EXCEPTION_IN_BEFORE);
		}

	}

	@Override
	public void afterKeyword(String keywordName,Keyword keyword) {
		output.add("afterKeyword",keywordName);
		if(getBooleanProperty(THROW_EXCEPTION_IN_AFTER)) {
			throw new RuntimeException(THROW_EXCEPTION_IN_AFTER);
		}
	}

	@Override
	public boolean onError(Exception e) {
		Throwable cause = e.getCause();
		output.add(ON_ERROR_MARKER, cause != null ? cause.getMessage() : e.getMessage());
		return getBooleanProperty(RETHROW_EXCEPTION_IN_ON_ERROR, true);
	}

	private boolean getBooleanProperty(String key) {
		return Boolean.parseBoolean(properties.getOrDefault(key, Boolean.FALSE.toString()));
	}

	private boolean getBooleanProperty(String key, boolean defaultValue) {
		return Boolean.parseBoolean(properties.getOrDefault(key, Boolean.toString(defaultValue)));
	}

	@Keyword
	public void MyKeyword() {
		output.add("test", "test");
	}

	@Keyword
	public Map<String, Serializable> MyKeywordWithReturn() {
		assert this.properties.get("myProperty").equals("myPropertyValue");
//		return output.build().getPayload(); --> won't work as not serializable
		return Map.of("key", "someStringValue","long", 12345798798L, "double", 123.456);
	}

	@Keyword
	public MyPojo MyKeywordWithReturnPojo() {
		assert this.properties.get("myProperty").equals("myPropertyValue");
		return new MyPojo();
	}

	@Keyword
	public PojoWithPrivateFields MyKeywordReturningPojoWithPrivateFields() {
		PojoWithPrivateFields pojoWithPrivateFields = new PojoWithPrivateFields();
		pojoWithPrivateFields.setStringField("some value");
		return pojoWithPrivateFields;
	}

	@Keyword
	public PojoWithPrivateFields MyKeywordWithReturnPojoAndOutputUsage() {
		output.add("test", "test");
		PojoWithPrivateFields pojoWithPrivateFields = new PojoWithPrivateFields();
		pojoWithPrivateFields.setStringField("some value");
		return pojoWithPrivateFields;
	}

	public static class MyPojo {
		public String stringField = "someValue";
		public long longValue = 123456787L;
		public boolean booleanValue = true;
		public double aoubleValue = 123.4567;
		public List<Object> someList = List.of("text", true, 12345678L);
		public Map<String, Object> someMap = Map.of("string", "value", "long", 123456748L, "boolean", true);
	}

	public static class PojoWithPrivateFields {
		private String stringField ;

		public String getStringField() {
			return stringField;
		}

		public void setStringField(String stringField) {
			this.stringField = stringField;
		}
	}

	
	@Keyword(name="My Keyword")
	public void MyKeywordWithCustomName() {
		output.add("test", "test");
	}
	
	@Keyword
	public void MyErrorKeyword() throws Exception {
		output.setError("My error");
	}
	
	@Keyword
	public void MyExceptionKeyword() throws Exception {
		throw new Exception("My exception");
	}
	
	@Keyword
	public void MyErrorKeywordWithThrowable() throws Throwable {
		throw new Throwable("My throwable");
	}
	
	@Keyword
	public void MyKeywordUsingProperties() {
		echoProperties();
	}
	
	@Keyword(properties = {"prop1"})
	public void MyKeywordWithPropertyAnnotation() {
		echoProperties();
	}
	
	@Keyword(properties = {"prop.{myPlaceHolder}"}, optionalProperties = {"myOptionalProperty"})
	public void MyKeywordWithPlaceHoldersInProperties() {
		echoProperties();
	}

	@Keyword(routing = {})
	public void MyKeywordWithDefaultRouting() {	}

	@Keyword(routing = {Keyword.ROUTING_EXECUTE_ON_CONTROLLER})
	public void MyKeywordWithRoutingToController() {	}

	@Keyword(routing = {"OS", "Windows","type","playwright"})
	public void MyKeywordWithRoutingToAgentsWithCriteria() {	}
	
	protected void echoProperties() {
		properties.entrySet().forEach(e->{
			output.add(e.getKey(), e.getValue());
		});
	}
	
	@Keyword
	public void MyKeywordUsingSession1() {
		session.put("object1","Test String");
		System.setProperty("testProperty", "test");
		session.put(new Closeable() {
			
			@Override
			public void close() throws IOException {
				System.clearProperty("testProperty");
			}
		});
	}

	@Keyword
	public void MyKeywordUsingSessionWithAutocloseable() {
		session.put("object1","Test String");
		System.setProperty("testProperty2", "test2");
		session.put(new AutoCloseable() {

			@Override
			public void close() throws IOException {
				System.clearProperty("testProperty2");
			}
		});
	}
	
	@Keyword
	public void MyKeywordUsingSession2() {
		output.add("sessionObject", (String)session.get("object1"));
	}

    // This is (almost) the same KW as we use in the documentation as a sample
    @Keyword /* Advanced error handling left of for the sake of simplicity */
    public void MyKeywordWithLiveReporting() throws IOException, InterruptedException {

        Path outputDir = null;
        if (liveReporting.fileUploads.getProvider() instanceof DiscardingStreamingUploadProvider) {
            outputDir = Files.createTempDirectory("output-");
            liveReporting = new LiveReporting(
                    new LocalDirectoryBackedStreamingUploadProvider(
                            Executors.newCachedThreadPool(),
                            outputDir.toFile()
                    ),
                    // redirect live measure to "regular" measure system, appended to output
                    new LiveMeasureSink() {
                        @Override
                        public void accept(Measure measure) {
                            output.addMeasure(measure);
                        }

                        @Override
                        public void close() {

                        }
                    }
            );
        }

        liveReporting.measures.startMeasure("realtime_measure");

        // This could also be a file produced by something else, like a log file/stdout of another process;
        // We're populating it ourselves here for demo purposes
        Path logFile = Files.createTempFile("logfile", ".txt");

        StreamingUpload upload = null;
        try {
            upload = liveReporting.fileUploads.startTextFileUpload(logFile.toFile());
        } catch (QuotaExceededException e) {
            // Your KW should be prepared to handle at least QuotaExceededException;
            output.add("quota error", e.getMessage());
        }

        // Log some demo data
        Files.writeString(logFile, "This is a line of text that can be streamed in realtime\n", StandardOpenOption.APPEND);
        Thread.sleep(1000);
        Files.writeString(logFile, "Another line.\n", StandardOpenOption.APPEND);

        // Uploads MUST be signaled to be complete!
        if (upload != null) {
            try {
                StreamingResourceStatus uploadStatus = upload.complete(Duration.ofSeconds(5));
                if (!uploadStatus.getTransferStatus().equals(StreamingResourceTransferStatus.COMPLETED)) {
                    output.add("unexpected transfer status", uploadStatus.getTransferStatus().name());
                }
            } catch (QuotaExceededException e) {
                // Again, be prepared to at least handle aborted uploads due to quota restrictions
                output.add("quota error", e.getMessage());
            } catch (TimeoutException | ExecutionException e) {
                output.add("unexpected error", e.getMessage());
            }
        }

        liveReporting.measures.stopMeasure();

        Files.deleteIfExists(logFile);
        // Recursively delete streaming target dir, while also capturing information about the "streamed" output
        if (outputDir != null) {
            try (var paths = Files.walk(outputDir)) {
                paths.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                if (path.toFile().isFile()) {
                                    // we're expecting to find exactly one file.
                                    output.add("uploadedFileLength", path.toFile().length());
                                }
                                Files.delete(path);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }
    }

}
