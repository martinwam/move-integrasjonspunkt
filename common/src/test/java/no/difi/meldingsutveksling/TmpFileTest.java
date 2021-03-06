package no.difi.meldingsutveksling;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class TmpFileTest {

    @Test
    public void testTmpFile() throws IOException {
        byte[] data = "foo".getBytes(StandardCharsets.UTF_8);
        TmpFile tmpFile = TmpFile.create();
        OutputStream os = tmpFile.getOutputStream();
        IOUtils.write(data, os);
        os.close();

        InputStream is = tmpFile.getInputStream();
        byte[] inBytes = IOUtils.toByteArray(is);
        is.close();
        assertThat(inBytes).isEqualTo(data);
        tmpFile.delete();
    }
}