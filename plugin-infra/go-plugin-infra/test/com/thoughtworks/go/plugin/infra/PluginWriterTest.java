package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.infra.commons.GoFileSystem;
import com.thoughtworks.go.plugin.infra.commons.PluginUploadResponse;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.util.SystemEnvironment.PLUGIN_EXTERNAL_PROVIDED_PATH;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluginWriterTest {

    @Mock
    private SystemEnvironment systemEnvironment;

    @Mock
    private GoFileSystem goFileSystem;

    @InjectMocks
    private PluginWriter pluginWriter = new PluginWriter(systemEnvironment, goFileSystem);

    private String EXTERNAL_DIRECTORY_PATH = "external_path";
    private File SRC_FILE = new File("a_plugin.jar");

    @Before
    public void init() throws IOException {
        initMocks(this);
        SRC_FILE.createNewFile();
    }

    @After
    public void clean() {
        FileUtils.deleteQuietly(SRC_FILE);
    }

    @Test
    public void shouldConstructCorrectDestinationFilePath() throws Exception {
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(EXTERNAL_DIRECTORY_PATH);

        pluginWriter.addPlugin(SRC_FILE, SRC_FILE.getName());

        ArgumentCaptor<File> srcfileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        ArgumentCaptor<File> destfileArgumentCaptor = ArgumentCaptor.forClass(File.class);
        verify(goFileSystem).copyFile(srcfileArgumentCaptor.capture(), destfileArgumentCaptor.capture());

        assertThat(srcfileArgumentCaptor.getValue(), is(SRC_FILE));
        assertThat(destfileArgumentCaptor.getValue().getName(), is(new File(EXTERNAL_DIRECTORY_PATH + "/" + SRC_FILE.getName()).getName()));
    }

    @Test
    public void shouldReturnSuccessResponseWhenSuccessfullyUploadedFile() throws Exception {
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(EXTERNAL_DIRECTORY_PATH);

        PluginUploadResponse response = pluginWriter.addPlugin(SRC_FILE, SRC_FILE.getName());

        assertTrue(response.isSuccess());
        assertThat(response.success(), is("Your file is saved!"));
    }

    @Test
    public void shouldReturnErrorResponseWhenFailedToUploadFile() throws Exception {
        when(systemEnvironment.get(PLUGIN_EXTERNAL_PROVIDED_PATH)).thenReturn(EXTERNAL_DIRECTORY_PATH);
        doThrow(new IOException()).when(goFileSystem).copyFile(any(File.class), any(File.class));

        PluginUploadResponse response = pluginWriter.addPlugin(SRC_FILE, SRC_FILE.getName());

        assertFalse(response.isSuccess());
        assertTrue(response.errors().containsKey(HttpStatus.INTERNAL_SERVER_ERROR.value()));

    }

}
