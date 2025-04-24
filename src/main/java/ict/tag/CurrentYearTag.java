package ict.tag; // Or your preferred package for tags

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.Calendar;

/**
 * A simple custom tag handler that outputs the current year.
 * Usage: <my:currentYear />
 */
public class CurrentYearTag extends SimpleTagSupport {

    @Override
    public void doTag() throws JspException, IOException {
        // Get the JSP writer to output content
        jakarta.servlet.jsp.JspWriter out = getJspContext().getOut();

        // Get the current year
        int year = Calendar.getInstance().get(Calendar.YEAR);

        // Write the year to the JSP output
        out.print(year);
    }
}
