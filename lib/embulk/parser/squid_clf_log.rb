Embulk::JavaPlugin.register_parser(
  "squid_clf_log", "org.embulk.parser.squid_clf_log.SquidClfLogParserPlugin",
  File.expand_path('../../../../classpath', __FILE__))
