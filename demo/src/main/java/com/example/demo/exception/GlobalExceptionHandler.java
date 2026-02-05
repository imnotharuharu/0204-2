package com.example.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import com.example.demo.service.exception.TodoNotFoundException;
import com.example.demo.service.exception.BusinessException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TodoNotFoundException.class)
    public ModelAndView handleNotFound(TodoNotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        ModelAndView mv = new ModelAndView("error/404");
        mv.addObject("message", ex.getMessage());
        return mv;
    }

    @ExceptionHandler(BusinessException.class)
    public ModelAndView handleBusiness(BusinessException ex, Model model) {
        log.warn("Business error: {}", ex.getMessage());
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("message", ex.getMessage());
        return mv;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleOther(Exception ex) {
        log.error("Unexpected error", ex);
        ModelAndView mv = new ModelAndView("error/500");
        mv.addObject("message", "予期しないエラーが発生しました。");
        return mv;
    }
}
