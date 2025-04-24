package ict.tag;

import java.io.IOException;
import java.util.Calendar;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;

public class CurrentYearTag extends SimpleTagSupport {

    @Override
    public void doTag() throws JspException, IOException {
        jakarta.servlet.jsp.JspWriter out = getJspContext().getOut();
        int year = Calendar.getInstance().get(Calendar.YEAR);
        out.print(year);
    }
}
