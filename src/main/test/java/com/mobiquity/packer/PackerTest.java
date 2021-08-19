package com.mobiquity.packer;

import com.mobiquity.exception.APIException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PackerTest {

    @Test
    void pack() throws APIException {
        File file = new File("src/main/test/resources/example_input");
        String filePath = file.getAbsolutePath();
        String result = Packer.pack(filePath);
        System.out.println(result);
    }
}