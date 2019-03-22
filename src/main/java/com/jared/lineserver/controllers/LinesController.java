package com.jared.lineserver.controllers;

import com.jared.lineserver.components.FileInitializer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
public class LinesController {
    FileInitializer fileInitializer = new FileInitializer();

    @RequestMapping(value="/lines/{id}", method=RequestMethod.GET)
    ResponseEntity<?> findOne(@PathVariable("id") int id){
        String line = fileInitializer.getLine(id);
        if(line == null){
            return new ResponseEntity<>(HttpStatus.valueOf(413));
        } else {
            return new ResponseEntity<>(line,HttpStatus.OK);
        }
    }
}
