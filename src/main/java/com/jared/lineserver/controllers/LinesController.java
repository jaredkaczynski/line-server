package com.jared.lineserver.controllers;

import com.jared.lineserver.components.FileInitializer;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;


@Controller
public class LinesController {
    //Controllers are a singleton and this is the only class using it so keep it simple, init here
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

    @ExceptionHandler(TypeMismatchException.class)
    public
    @ResponseBody
    ResponseEntity<?> handleMyException(Exception exception, HttpServletRequest request) {
        return new ResponseEntity<>(HttpStatus.valueOf(413));
    }

}
