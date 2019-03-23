package com.jared.lineserver;

import com.jared.lineserver.controllers.LinesController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
public class LineServerApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void contextLoads() {
	}

	@Test
	public void outOfRangeTest() throws Exception {
		this.mockMvc.perform(get("/lines/-1")).andExpect(status().is(413))
				.andExpect(content().string(""));
	}

	@Test
	public void inRangeTest0() throws Exception {
		this.mockMvc.perform(get("/lines/0")).andExpect(status().is(200))
				.andExpect(content().string("0"));
	}

	@Test
	public void inRangeTest55555() throws Exception {
		this.mockMvc.perform(get("/lines/55555")).andExpect(status().is(200))
				.andExpect(content().string("55555"));
	}

}
