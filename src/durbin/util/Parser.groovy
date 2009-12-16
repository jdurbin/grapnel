//package org.computoring.gop
package durbin.util

/*********************************************************
* Durbin fork!!
* 
* GOP is nice, but I didn't like the fact that it didn't handle a help option 
* (that is, it'd always print out the required missing list, which was distracting)
* 
* I also messed with the formatting a little here and there.  
* 
*/ 


/**
 * Groovy Option Parser
 *
 * GOP is a command line option parser alternative to CliBuilder.
 * 
 * An example:
 * <pre>
 *  def parser = new org.computoring.gop.Parser(description: "An example parser.")
 *  parser.with {
 *    required 'f', 'foo-bar', [description: 'The foo-bar option'] 
 *    optional 'b', [
 *      longName: 'bar-baz', 
 *      default: 'xyz', 
 *      description: 'The optional bar-baz option with a default of "xyz"'
 *    ]
 *    flag 'c' 
 *    flag 'd', 'debug', [default: true] 
 *    required 'i', 'count', [
 *      description: 'A required, validated option', 
 *      validate: {
 *        Integer.parseInt it 
 *      }
 *    ] 
 *    remainder {
 *      assert it
 *      it
 *    }
 *  }                                                                                                                                                                                                             
 *
 *  def params = parser.parse("-f foo_value --debug --count 123 -- some other stuff".split())
 *  assert params.'foo-bar' == 'foo_value'
 *  assert params.b == 'xyz'
 *  assert params.c == false
 *  assert params.debug == true
 *  assert params.count instanceof Integer
 *  assert params.i == 123
 *  assert parser.remainder.join(' ') == 'some other stuff'
 * </pre>
 * 
 * <b>NOTICE: Durbin fork!!</b><br><br>
 * 
 *  GOP is nice, but I didn't like the fact that it didn't handle a help option 
 * (that is, it'd always print out the required missing list, which was distracting)
 * I also messed with the formatting a little here and there.<br><br>
 * 
 *
 * @author Travis Hume (travis@computoring.org)
 */
public class Parser {

  /** A property describing this option parser.  Displayed in usage statement. */
  def description

  /**
   * A property specifying the max width for option defaults when formatting
   * the usage statement.
   */
  int defaultValueWidth = 30

  def options = [:]
  def parameters = [:]
  def remainder = []

  private Closure remainderValidator
  private Closure postParseValidator
  private Throwable remainderError
  private Throwable postParseError
  private boolean parseCalled

  /**
   * Add a required option to the parser.
   * Parameters must be supplied for each required option at parsing time.
   *
   * @param shortName
   *        A single character name to use with for this option.  Pass in <code>null</code> to specify
   *        an option without a shortName.
   *
   * @param opts
   *        A map of additional options for this option.  Recognized options include:
   *        longName: String    -- A long name to use for this option.  Long names can be anything, but you
   *                               have to follow groovy rules of map key referencing, namely quoting anything
   *                               that isn't a simple string (i.e. params.'long-option')
   *        description: String -- A string describing this option.  This description will be used to create a
   *                               usage statement.
   *        validate: {Closure} -- A closure that will be passed the parameter supplied for this
   *                               option.  The return value of the closure is the final value of
   *                               the parameter.  This is useful for conversions and validations.
   *
   * @throws Exception
   */
  def required( String shortName, Map opts = [:] ) {
    if( opts.default ) {
      throw new Exception( "Default values don't make sense for required options" )
    }

    addOption( shortName, 'required', opts )
  }

  /**
   * A convienence method for required( shortName, [longName: name] ).
   * @see #required( String, Map )
   */
  def required( String shortName, String longName, Map opts = [:] ) {
    required( shortName, [longName: longName] + opts )
  }

  /**
   * Add an optional option to the parser.
   * Parameters are not required to be supplied for optional options at parsing time.  Additionally, optional
   * options may have a default value.
   *
   * @param shortName
   *        A single character name to use with for this option.  Pass in <code>null</code> to specify
   *        an option without a shortName.
   *
   * @param opts
   *        A map of additional options for this option.  Recognized options include:
   *        longName: String    -- A long name to use for this option.  Long names can be anything, but you
   *                               have to follow groovy rules of map key referencing, namely quoting anything
   *                               that isn't a simple string (i.e. params.'long-option')
   *        default: value      -- A default value to return if none is provided.  Note that the a default value
   *                               is processed by the validate closure is one is specified.
   *        description: String -- A string describing this option.  This description will be used to create a
   *                               usage statement.
   *        validate: {Closure} -- A closure that will be passed the parameter supplied for this
   *                               option.  The return value of the closure is the final value of
   *                               the parameter.  This is useful for conversions and validations.
   *
   * @throws Exception
   */
  def optional( String shortName, Map opts = [:] ) {
    addOption( shortName, 'optional', opts )
  }

  /**
   * A convienence method for optional( shortName, [longName: name] ).
   * @see #optional( String, Map )
   */
  def optional( String shortName, String longName, Map opts = [:] ) {
    optional( shortName, [longName: longName] + opts )
  }

  /**
   * Add a flag option to the parser.
   * Flags are boolean options that do not accept a value during parsing.  Flags are false by default and specifying
   * them during parsing will make them true.  Default value can be changed to true, see below.
   *
   * @param shortName
   *        A single character name to use with for this option.  Pass in <code>null</code> to specify
   *        an option without a shortName.
   *
   * @param opts
   *        A map of additional options for this option.  Recognized options include:
   *        longName: String    -- A long name to use for this option.  Long names can be anything, but you
   *                               have to follow groovy rules of map key referencing, namely quoting anything
   *                               that isn't a simple string (i.e. params.'long-option')
   *        default: value      -- The default value will be true or false depending on the 
   *                               truthiness of the supplied value.
   *        description: String -- A string describing this option.  This description will be used to create a
   *                               usage statement.
   *        validate: {Closure} -- A closure that will be passed the parameter supplied for this
   *                               option.  The return value of the closure is evaluated as true or false and assigned
   *                               to the parameter.
   *
   * @throws Exception
   */
  def flag( String shortName, Map opts = [:] ) {
    opts.default = ( opts.default ) ? true : false
    addOption( shortName, 'flag', opts )
  }

  /**
   * A convienence method for flag( shortName, [longName: name] ).
   * @see #flag( String, Map )
   */
  def flag( String shortName, String longName, Map opts = [:] ) {
    flag( shortName, [longName: longName] + opts )
  }

  /**
   * Define a validation closure for the remainder.
   *
   * @param validator -- A closure that will be passed the remainder after parameter parsing is complete.
   *                     The return value of the closure is the final value of the remainder.
   *                     This is useful for conversions and validations.
   */
  def remainder( Closure validator ) {
    this.remainderValidator = validator
  }

  /**
   * Define a post parse validation closure.
   * After parsing, this Closure will be ran and the parsed parameters passed to it.  Useful
   * for validating inter-option dependencies (e.g. must specify --this or --that)
   * 
   * @param validator -- A Closure that will be passed the parsed parameters.
   */
  def validate( Closure validator ) {
    this.postParseValidator = validator
  }

  /**
   * Apply configured options to the supplied args returning a map of parameters.
   * Each option that is mapped to a parameter is available in the returned map in its
   * short and optionally its long name.
   *
   * @param args
   *        Typically, an array of command line arguments.  Can be any Iterable. 
   *
   * @return Map
   *         A map of parsed parameters.  Each option that is mapped to a parameter will have an
   *         entry for its shortName and additionally for its longName if specified.
   *
   * @throws Exception
   */
  Map parse( args ) {
    parseCalled = true

    def PARAM_NAME = ~/^(-[^-]|--.+)$/
    def parameter = null
    args.each { arg ->
      if( remainder ) {
        remainder << arg
      }
      else if( parameter ) {
        addParameter( parameter, arg )
        parameter = null
      }
      else if( arg =~ PARAM_NAME ) {
        // options can't look like -foo
        if( arg =~ ~/^-[^-].+/ ) {
          throw new Exception( "Illegal parameter [$arg], short options must be a single character" )
        }

        def name = arg.replaceFirst( /--?/, '' )
        if( !options.containsKey( name )) {
          throw new Exception( "unknown parameter $arg" )
        }

        parameter = options[name]
        if( parameter.type == 'flag' ) {
          addParameter( parameter, true )
          parameter = null
        }
      }
      else {
        remainder << arg
//        if( !( arg == '--' )) remainder << arg
      }
    }

    // if we found -- to stop parsing, remove it
    if( remainder[0] == '--' ) remainder = remainder[1..-1]

    if( missingOptions ) {
      throw new Exception( "Required parameters not provided" )
    }

    if( errorOptions ) {
      throw new Exception( "Validation errors" )
    }

    if( remainderValidator ) {
      try {
        remainder = remainderValidator(remainder)
      }
      catch( Throwable t ) {
        remainderError = t
        throw new Exception( "Remainder validation error", t)
      }
    }

    if( postParseValidator ) {
      try {
        postParseValidator( parameters )
      }
      catch( Throwable t ) {
        postParseError = t
        throw new Exception( "Post parsing validation failure", t )
      }
    }

    return parameters
  }

  /**
   * Returns a formatted String describing this parser's options with their default values and
   * descriptions.
   *
   * Note that effort is made to align defaults and descriptinos vertically.  This can be a bit
   * wonky if you supply a large default or description.
   *
   * @param message
   *        When supplied, message will be displayed at the beginning of the usage message.
   *        Useful for reporting exceptions during parsing or values that fail option validation.
   */
  String getUsage() {
    def buffer = new StringWriter()
    def writer = new PrintWriter( buffer )

    def missing = missingOptions
    def errors = errorOptions
    if( parseCalled && (missing || errors || remainderError || postParseError)) {
      if(missing) {
        // Don't print the list if help option was given...
        if (!parameters['help']){
          writer.println( "Missing required parameter(s)" )
          missing.each {
            def option = it.shortName ? "-$it.shortName" : "--$it.longName"
            writer.println( "  ${( it.description ) ? "$option $it.description" : "$option"}" )
          }
        }
      }

      if( errors ) {
        writer.println( "Validation errors" )
        errors.each {
          def option = it.shortName ? "-$it.shortName" : "--$it.longName"
          writer.println( "  $option : ${it.error.toString()}" )
        }
      }

      if( remainderError ) {
        writer.println( "Remainder validation error" )
        writer.println( "  $remainderError" )
      }

      if( postParseError ) {
        writer.println( "" )
        writer.println( "Option validation error:" )
        writer.println( "  ${postParseError.getMessage()}" )
      }

      //writer.println( "" )
    }

    if( description ) writer.println( description )

    def longestName = 5 + options.inject( 0 ) { max, option -> 
      option.value.longName ? Math.max( max, option.value.longName.size() ) : max
    }

    def longestDefault = 5 + options.inject( 0 ) { max, option -> 
      def x = option.value.default
      (x && x.metaClass.respondsTo(x, "size")) ? Math.max( max, x.size() ) : max
    }

    def pattern = "  %s%s%-${longestName}s %-${longestDefault}s %s\n"
    ['Required': requiredOptions, 'Optional': optionalOptions, 'Flags': flagOptions].each { header, map ->
      if( map ) {
        writer.println( header )
        map.each { name, opts ->
          def shortName = opts.shortName ? "-$opts.shortName" : "  "
          def comma = (opts.shortName && opts.longName) ? ", " : "  "
          def longName = opts.longName ? "--$opts.longName" : ""
          def defaultValue = (opts.default || opts.type == 'flag') ? "[${opts.default.toString()}]" : ""
          if(defaultValue.size() > defaultValueWidth) {
            defaultValue = "[${defaultValue[1..defaultValueWidth]}...]"
          }
          def description = opts.description ?: ""

          writer.printf( pattern, shortName, comma, longName, defaultValue, description)
        }

        writer.println()
      }
    }

    return buffer.toString()
  }

  private def getMissingOptions() {
    (requiredOptions.keySet() - parameters.keySet()).inject( [] ) {list, it ->
      list << options[it]
      list
    }
  }

  private def getErrorOptions() {
    options.values().findAll { it.error } as Set
  }

  private def addParameter( parameter, value ) {
    if( parameter.validate ) {
      try {
        value = parameter.validate( value )
        if( parameter.type == 'flag' ) value = value ? true : false
      }
      catch( Throwable t ) {
        def x = options[parameter.shortName] ?: options[parameter.longName]
        x.error = t
        value = null
      }
    }

    if( parameter.shortName ) parameters[parameter.shortName] = value
    if( parameter.longName ) parameters[parameter.longName] = value
  }

  private def getRequiredOptions() {
    findOptions( 'required' )
  }

  private def getOptionalOptions() {
    findOptions( 'optional' )
  }

  private def getFlagOptions() {
    findOptions( 'flag' )
  }

  private def findOptions( type ) {
    options.inject( [:] ) { map, option ->
      if( option.value.type == type ) {
        def name = option.value.shortName ?: option.value.longName
        map[name] = option.value
      }

      map
    }
  }

  private def addOption( shortName, type, opts ) {
    opts = opts ?: [:]
    opts.type = type ?: 'optional'

    if( !shortName && !opts.longName ) {
      throw new Exception( "Not short or long name specified" )
    }

    if( shortName && options.containsKey(shortName)) {
      throw new Exception( "Dup option specified: $shortName" )
    }

    if( shortName && shortName.size() != 1 ) {
      throw new Exception( "Invalid option name: $shortName.  Option names must be a single character.  To set a long name for this option add [longName: 'long-name']" )
    }

    if( opts.longName && options.containsKey(opts.longName)) {
      throw new Exception( "Dup option specified: $opts.longName" )
    }

    if( opts.validate && !(opts.validate instanceof Closure )) {
      throw new Exception( "Invalid validate option, must be a Closure" )
    }

    if( shortName ) {
      opts.shortName = shortName
      options[shortName] = opts
    }

    if( opts.longName ) {
      options[opts.longName] = opts
    }

    // create parameters for options with defaults
    if(opts.containsKey("default")) {
      addParameter(opts, opts.default)
    }
  }

  private setOptions( arg ) {}
  private setUsage( arg ) {}
}