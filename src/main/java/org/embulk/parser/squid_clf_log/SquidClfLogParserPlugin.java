package org.embulk.parser.squid_clf_log;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskSource;
import org.embulk.spi.ParserPlugin;
import org.embulk.spi.FileInput;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfig;

import org.embulk.spi.Exec;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.util.LineDecoder;
import org.embulk.spi.type.TimestampType;
import org.embulk.spi.time.TimestampParser;
import org.embulk.spi.time.TimestampParseException;
import org.embulk.spi.ColumnConfig;
import java.util.ArrayList;

//import static org.embulk.spi.type.Types.BOOLEAN;
//import static org.embulk.spi.type.Types.DOUBLE;
//import static org.embulk.spi.type.Types.LONG;
import static org.embulk.spi.type.Types.STRING;
import static org.embulk.spi.type.Types.TIMESTAMP;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Throwables;

import org.slf4j.Logger;

public class SquidClfLogParserPlugin
        implements ParserPlugin
{
    private static final Logger logger = Exec.getLogger(SquidClfLogParserPlugin.class);

    public enum LogFormat
    {
         combined("combined"),
         common("common");
         private final String string;

         private LogFormat(String string)
         {
             this.string = string;
         }
         public String getString()
         {
             return string;
         }
    }
    public interface PluginTask
            extends Task, LineDecoder.DecoderTask, TimestampParser.Task
    {

        @Config("format")
        @ConfigDefault("\"combined\"")
        public LogFormat getFormat();

        @Config("stop_on_invalid_record")
        @ConfigDefault("true")
        Boolean getStopOnInvalidRecord();
    }

    @Override
    public void transaction(ConfigSource config, ParserPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);
        ArrayList<ColumnConfig> columns = new ArrayList<ColumnConfig>();
        final LogFormat format = task.getFormat();

        columns.add(new ColumnConfig("client-src-ip-address" ,STRING   ,config));
        columns.add(new ColumnConfig("request-username-ident",STRING   ,config));
        columns.add(new ColumnConfig("request-username"      ,STRING   ,config));
        columns.add(new ColumnConfig("request-time"          ,TIMESTAMP,config));
        columns.add(new ColumnConfig("request-method"        ,STRING   ,config));
        columns.add(new ColumnConfig("request-url"           ,STRING   ,config));
        columns.add(new ColumnConfig("request-protocol"      ,STRING   ,config));
        columns.add(new ColumnConfig("response-status"       ,STRING   ,config));
        columns.add(new ColumnConfig("response-bytes"        ,STRING   ,config));

        // combined
        if( format == LogFormat.combined ){
          columns.add(new ColumnConfig("referer"             ,STRING   ,config));
          columns.add(new ColumnConfig("user-agent"          ,STRING   ,config));
        }

        // squid status
        columns.add(new ColumnConfig("squid-status"          ,STRING   ,config));
        columns.add(new ColumnConfig("squid-hierarchy-status",STRING   ,config));

        Schema schema = new SchemaConfig(columns).toSchema();
        control.run(task.dump(), schema);
    }

    private static interface ParserIntlTask extends Task, TimestampParser.Task {}
    private static interface ParserIntlColumnOption extends Task, TimestampParser.TimestampColumnOption {}

    @Override
    public void run(TaskSource taskSource, Schema schema,
            FileInput input, PageOutput output)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        LineDecoder lineDecoder = new LineDecoder(input,task);
        PageBuilder pageBuilder = new PageBuilder(Exec.getBufferAllocator(), schema, output);
        String line = null;
        final LogFormat format = task.getFormat();

        Pattern accessLogPattern = Pattern.compile(getAccessLogRegex(format),
                                                     Pattern.CASE_INSENSITIVE
                                                   | Pattern.DOTALL);
        Matcher accessLogEntryMatcher;
        // TODO: Switch to a newer TimestampParser constructor after a reasonable interval.
        // Traditional constructor is used here for compatibility.
        final ConfigSource configSource = Exec.newConfigSource();
        configSource.set("format", "%d/%b/%Y:%T %z");
        configSource.set("timezone", task.getDefaultTimeZone());
        final TimestampParser time_parser = new TimestampParser(
            Exec.newConfigSource().loadConfig(ParserIntlTask.class),
            configSource.loadConfig(ParserIntlColumnOption.class));

        while( input.nextFile() ){
            while(true){
              line = lineDecoder.poll();

              if( line == null ){
                  break;
              }
              accessLogEntryMatcher = accessLogPattern.matcher(line);

              if(!accessLogEntryMatcher.matches()){
                if (task.getStopOnInvalidRecord()) {
                  throw new RuntimeException("unmatched line" + line);
                } else {
                  logger.info("unable to parse line: " + line);
                  continue;
                }
              }

              pageBuilder.setString(0,accessLogEntryMatcher.group(1));
              pageBuilder.setString(1,accessLogEntryMatcher.group(2));
              pageBuilder.setString(2,accessLogEntryMatcher.group(3));
              try {
                  pageBuilder.setTimestamp(3,time_parser.parse(accessLogEntryMatcher.group(4)));
              } catch(TimestampParseException ex) {
                if (task.getStopOnInvalidRecord()) {
                  throw Throwables.propagate(ex);
                } else {
                  logger.info("unable to parse time from line: " + line);
                  continue;
                }
              }
              pageBuilder.setString(4,accessLogEntryMatcher.group(5));
              pageBuilder.setString(5,accessLogEntryMatcher.group(6));
              pageBuilder.setString(6,accessLogEntryMatcher.group(7));
              pageBuilder.setString(7,accessLogEntryMatcher.group(8));
              pageBuilder.setString(8,accessLogEntryMatcher.group(9));
              pageBuilder.setString(9,accessLogEntryMatcher.group(10));
              pageBuilder.setString(10,accessLogEntryMatcher.group(11));
              if( format == LogFormat.combined ){
                pageBuilder.setString(11,accessLogEntryMatcher.group(12));
                pageBuilder.setString(12,accessLogEntryMatcher.group(13));
              }
              pageBuilder.addRecord();
            }
        }
        pageBuilder.finish();
    }

    private String getAccessLogRegex(LogFormat type)
    {
        final String ipaddr    = "(\\d+(?:\\.\\d+){3})";                      // an IP address
        final String nospace   = "(\\S+)";                                    // a single token (no spaces)
        final String timestamp = "\\[([^\\]]+)\\]";                           // something between [ and ]
        final String quotestr  = "\"(.*?)\"";                                 // a quoted string
        final String uint      = "(\\d+)";                                    // unsigned integer
        final String query     = "\"(\\S+)\\s(.*?)\\s(HTTP\\/\\d+\\.\\d+)\""; // method, path, protocol
        final String sqstat    = "(\\S+)\\:(\\S+)";                           // squid status

        String rex;

        if( type == LogFormat.combined ){
          rex = "^" + String.join( " ", ipaddr, nospace, nospace, timestamp, query,
                             uint, nospace, quotestr, quotestr, sqstat) + "$";
        } else {
          rex = "^" + String.join( " ", ipaddr, nospace, nospace, timestamp, query,
                             uint, nospace, sqstat) + "$";
        }

        return rex;
   }
}
