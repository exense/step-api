package step.reporting;

/**
 * Utility class defining common MIME types used in Step
 */
public class CommonMimeTypes {
    /** MIME type for plain text. */
    public static final String TEXT_PLAIN = "text/plain";

    /** MIME type for HTML text. */
    public static final String TEXT_HTML = "text/html";

    /** MIME type for JSON documents. */
    public static final String APPLICATION_JSON = "application/json";

    /** Generic MIME type for binary data. */
    public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    /** MIME type for zipped Playwright traces (Step). */
    public static final String APPLICATION_PLAYWRIGHT_TRACE = "application/vnd.step.playwright-trace+zip";

    private CommonMimeTypes() {}
}
