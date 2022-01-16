package com.doubledee.ultrastar;

import com.doubledee.ultrastar.importer.SongImporter;
import com.doubledee.ultrastar.models.Song;
import net.davidashen.text.Hyphenator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
public class UltrastarApplication {

	public static List<Song> SONGS;
	public static void main(String[] args) {
		SpringApplication.run(UltrastarApplication.class, args);
		SONGS = new SongImporter().getImportedSongs();
	}

	public static void hyphenate(String[] args) throws IOException, UnsupportedFlavorException {
		String data = (String) Toolkit.getDefaultToolkit()
				.getSystemClipboard().getData(DataFlavor.stringFlavor);
		Hyphenator hyphenator = new Hyphenator();
		System.out.println(hyphenator.hyphenate(data));
	}
}
