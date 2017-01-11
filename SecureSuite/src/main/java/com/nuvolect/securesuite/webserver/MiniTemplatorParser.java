package com.nuvolect.securesuite.webserver;//

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

// MiniTemplatorParser is an immutable object that contains the parsed template text.
public class MiniTemplatorParser {

//--- constants ------------------------------------------------------

    private static final int     maxNestingLevel  = 20;        // maximum number of block nestings
    private static final int     maxCondLevels    = 20;        // maximum number of nested conditional commands ($if)
    private static final int     maxInclTemplateSize = 1000000; // maximum length of template string when including subtemplates
    private static final String  cmdStartStr      = "<!--";    // command start string
    private static final String  cmdEndStr        = "-->";     // command end string
    private static final String  cmdStartStrShort = "<$";      // short form command start string
    private static final String  cmdEndStrShort   = ">";       // short form command end string

//--- nested classes -------------------------------------------------

    public static class VarRefTabRec {                         // variable reference table record structure
        int                       varNo;                        // variable no
        int                       tPosBegin;                    // template position of begin of variable reference
        int                       tPosEnd;                      // template position of end of variable reference
        int                       blockNo;                      // block no of the (innermost) block that contains this variable reference
        int                       blockVarNo; }                 // block variable no. Index into BlockInstTab.BlockVarTab
    public static class BlockTabRec {                          // block table record structure
        String                    blockName;                    // block name
        int                       nextWithSameName;             // block no of next block with same name or -1 (blocks are backward linked related to their position within the template)
        int                       tPosBegin;                    // template position of begin of block
        int                       tPosContentsBegin;            // template pos of begin of block contents
        int                       tPosContentsEnd;              // template pos of end of block contents
        int                       tPosEnd;                      // template position of end of block
        int                       nestingLevel;                 // block nesting level
        int                       parentBlockNo;                // block no of parent block
        boolean                   definitionIsOpen;             // true while $BeginBlock processed but no $EndBlock
        int                       blockVarCnt;                  // number of variables in block
        int[]                     blockVarNoToVarNoMap;         // maps block variable numbers to variable numbers
        int                       firstVarRefNo;                // variable reference no of first variable of this block or -1
        boolean                   dummy; }                      // true if this is a dummy block that will never be included in the output

//--- variables ------------------------------------------------------

    public  String               templateText;                 // contents of the template file
    private HashSet<String> conditionFlags;               // set of the condition flags, converted to uppercase
    private boolean              shortFormEnabled;             // true to enable the short form of commands ("<$...>")

    public  String[]             varTab;                       // variables table, contains variable names, array index is variable no
    public  int                  varTabCnt;                    // no of entries used in VarTab
    private HashMap<String,Integer> varNameToNoMap;            // maps variable names to variable numbers
    public  VarRefTabRec[]       varRefTab;                    // variable references table
    // Contains an entry for each variable reference in the template. Ordered by templatePos.
    public  int                  varRefTabCnt;                 // no of entries used in VarRefTab

    public  BlockTabRec[]        blockTab;                     // Blocks table, array index is block no
    // Contains an entry for each block in the template. Ordered by tPosBegin.
    public  int                  blockTabCnt;                  // no of entries used in BlockTab
    private HashMap<String,Integer> blockNameToNoMap;          // maps block names to block numbers

    // The following variables are only used temporarilly during parsing of the template.
    private int                  currentNestingLevel;          // current block nesting level during parsing
    private int[]                openBlocksTab;                // indexed by the block nesting level
    // During parsing, this table contains the block numbers of the open parent blocks (nested outer blocks).
    private int                  condLevel;                    // current nesting level of conditional commands ($if), -1 = main level
    private boolean[]            condEnabled;                  // enabled/disables state for the conditions of each level
    private boolean[]            condPassed;                   // true if an enabled condition clause has already been processed (separate for each level)
    private MiniTemplator miniTemplator;                // the MiniTemplator who created this parser object
    // The reference to the MiniTemplator object is only used to call MiniTemplator.loadSubtemplate().
    private boolean              resumeCmdParsingFromStart;    // true = resume command parsing from the start position of the last command

//--- constructor ----------------------------------------------------

    // (The MiniTemplator object is only passed to the parser, because the
// parser needs to call MiniTemplator.loadSubtemplate() to load subtemplates.)
    public MiniTemplatorParser (String templateText, Set<String> conditionFlags, boolean shortFormEnabled, MiniTemplator miniTemplator)
            throws MiniTemplator.TemplateSyntaxException {
        this.templateText = templateText;
        this.conditionFlags = createConditionFlagsSet(conditionFlags);
        this.shortFormEnabled = shortFormEnabled;
        this.miniTemplator = miniTemplator;
        parseTemplate();
        this.miniTemplator = null; }

    private HashSet<String> createConditionFlagsSet (Set<String> flags) {
        if (flags == null || flags.isEmpty()) {
            return null; }
        HashSet<String> flags2 = new HashSet<String>(flags.size());
        for (String flag : flags) {
            flags2.add (flag.toUpperCase()); }
        return flags2; }

//--- template parsing -----------------------------------------------

    private void parseTemplate()
            throws MiniTemplator.TemplateSyntaxException {
        initParsing();
        beginMainBlock();
        parseTemplateCommands();
        endMainBlock();
        checkBlockDefinitionsComplete();
        if (condLevel != -1) {
            throw new MiniTemplator.TemplateSyntaxException ("$if without matching $endIf."); }
        parseTemplateVariables();
        associateVariablesWithBlocks();
        terminateParsing(); }

    private void initParsing() {
        varTab = new String[64];
        varTabCnt = 0;
        varNameToNoMap = new HashMap<String,Integer>();
        varRefTab = new VarRefTabRec[64];
        varRefTabCnt = 0;
        blockTab = new BlockTabRec[16];
        blockTabCnt = 0;
        currentNestingLevel = 0;
        blockNameToNoMap = new HashMap<String,Integer>();
        openBlocksTab = new int[maxNestingLevel+1];
        condLevel = -1;
        condEnabled = new boolean[maxCondLevels];
        condPassed = new boolean[maxCondLevels]; }

    private void terminateParsing() {
        openBlocksTab = null; }

    // Registers the main block.
// The main block is an implicitly defined block that covers the whole template.
    private void beginMainBlock() {
        int blockNo = registerBlock(null);                      // =0
        BlockTabRec btr = blockTab[blockNo];
        btr.tPosBegin = 0;
        btr.tPosContentsBegin = 0;
        openBlocksTab[currentNestingLevel] = blockNo;
        currentNestingLevel++; }

    // Completes the main block registration.
    private void endMainBlock() {
        BlockTabRec btr = blockTab[0];
        btr.tPosContentsEnd = templateText.length();
        btr.tPosEnd = templateText.length();
        btr.definitionIsOpen = false;
        currentNestingLevel--; }

//--- Template commands --------------------------------------------------------

    // Parses commands within the template in the format "<!-- $command parameters -->".
// If shortFormEnabled is true, the short form commands in the format "<$...>" are also recognized.
    private void parseTemplateCommands()
            throws MiniTemplator.TemplateSyntaxException {
        int p = 0;                                              // p is the current position within templateText
        while (true) {
            int p0 = templateText.indexOf(cmdStartStr, p);       // p0 is the start of the current command
            boolean shortForm = false;
            if (shortFormEnabled && p0 != p) {
                if (p0 == -1) {
                    p0 = templateText.indexOf(cmdStartStrShort, p);
                    shortForm = true; }
                else {
                    int p2 = templateText.substring(p, p0).indexOf(cmdStartStrShort);
                    if (p2 != -1) {
                        p0 = p + p2;
                        shortForm = true; }}}
            if (p0 == -1) {                                      // no more commands
                break; }
            conditionalExclude(p, p0);                           // process text up to the start of the current command
            if (shortForm) {                                     // short form command
                p = templateText.indexOf(cmdEndStrShort, p0 + cmdStartStrShort.length());
                if (p == -1) {                                    // if no terminating ">" is found, we process it as normal text
                    p = p0 + cmdStartStrShort.length();
                    conditionalExclude(p0, p);
                    continue; }
                p += cmdEndStrShort.length();
                String cmdLine = templateText.substring(p0 + cmdStartStrShort.length(), p - cmdEndStrShort.length());
                if (!processShortFormTemplateCommand(cmdLine, p0, p)) {
                    // If a short form command is not recognized, we process the whole command structure are normal text.
                    conditionalExclude(p0, p); }}
            else {                                              // normal (long) form command
                p = templateText.indexOf(cmdEndStr, p0 + cmdStartStr.length());
                if (p == -1) {
                    throw new MiniTemplator.TemplateSyntaxException("Invalid HTML comment in template at offset " + p0 + "."); }
                p += cmdEndStr.length();
                String cmdLine = templateText.substring(p0 + cmdStartStr.length(), p - cmdEndStr.length());
                resumeCmdParsingFromStart = false;
                if (!processTemplateCommand(cmdLine, p0, p)) {
                    conditionalExclude(p0, p); }                   // process as normal temlate text
                if (resumeCmdParsingFromStart) {                  // (if a subtemplate has been included)
                    p = p0; }}}}

    // Returns false if the command should be treatet as normal template text.
    private boolean processTemplateCommand (String cmdLine, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        int p0 = skipBlanks(cmdLine, 0);
        if (p0 >= cmdLine.length()) {
            return false; }
        int p = skipNonBlanks(cmdLine, p0);
        String cmd = cmdLine.substring(p0, p);
        String parms = cmdLine.substring(p);
   /* select */
        if (cmd.equalsIgnoreCase("$beginBlock")) {
            processBeginBlockCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equalsIgnoreCase("$endBlock")) {
            processEndBlockCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equalsIgnoreCase("$include")) {
            processIncludeCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equalsIgnoreCase("$if")) {
            processIfCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equalsIgnoreCase("$elseIf")) {
            processElseIfCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equalsIgnoreCase("$else")) {
            processElseCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equalsIgnoreCase("$endIf")) {
            processEndIfCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else {
            if (cmd.startsWith("$") && !cmd.startsWith("${")) {
                throw new MiniTemplator.TemplateSyntaxException("Unknown command \"" + cmd + "\" in template at offset " + cmdTPosBegin + "."); }
            else {
                return false; }}
        return true; }

    // Returns false if the command is not recognized and should be treatet as normal temlate text.
    private boolean processShortFormTemplateCommand (String cmdLine, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        int p0 = skipBlanks(cmdLine, 0);
        if (p0 >= cmdLine.length()) {
            return false; }
        int p = p0;
        char cmd1 = cmdLine.charAt(p++);
        if (cmd1 == '/' && p < cmdLine.length() && !Character.isWhitespace(cmdLine.charAt(p))) {
            p++; }
        String cmd = cmdLine.substring(p0, p);
        String parms = cmdLine.substring(p).trim();
   /* select */
        if (cmd.equals("?")) {
            processIfCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else if (cmd.equals(":")) {
            if (parms.length() > 0) {
                processElseIfCmd(parms, cmdTPosBegin, cmdTPosEnd); }
            else {
                processElseCmd(parms, cmdTPosBegin, cmdTPosEnd); }}
        else if (cmd.equals("/?")) {
            processEndIfCmd(parms, cmdTPosBegin, cmdTPosEnd); }
        else {
            return false; }
        return true; }

    // Processes the $beginBlock command.
    private void processBeginBlockCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        if (conditionalExclude(cmdTPosBegin, cmdTPosEnd)) {
            return; }
        int p0 = skipBlanks(parms, 0);
        if (p0 >= parms.length()) {
            throw new MiniTemplator.TemplateSyntaxException("Missing block name in $BeginBlock command in template at offset " + cmdTPosBegin + "."); }
        int p = skipNonBlanks(parms, p0);
        String blockName = parms.substring(p0, p);
        if (!isRestOfStringBlank(parms, p)) {
            throw new MiniTemplator.TemplateSyntaxException("Extra parameter in $BeginBlock command in template at offset " + cmdTPosBegin + "."); }
        int blockNo = registerBlock(blockName);
        BlockTabRec btr = blockTab[blockNo];
        btr.tPosBegin = cmdTPosBegin;
        btr.tPosContentsBegin = cmdTPosEnd;
        openBlocksTab[currentNestingLevel] = blockNo;
        currentNestingLevel++;
        if (currentNestingLevel > maxNestingLevel) {
            throw new MiniTemplator.TemplateSyntaxException("Block nesting overflow for block \"" + blockName + "\" in template at offset " + cmdTPosBegin + "."); }}

    // Processes the $endBlock command.
    private void processEndBlockCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        if (conditionalExclude(cmdTPosBegin, cmdTPosEnd)) {
            return; }
        int p0 = skipBlanks(parms, 0);
        if (p0 >= parms.length()) {
            throw new MiniTemplator.TemplateSyntaxException("Missing block name in $EndBlock command in template at offset " + cmdTPosBegin + "."); }
        int p = skipNonBlanks(parms, p0);
        String blockName = parms.substring(p0, p);
        if (!isRestOfStringBlank(parms, p)) {
            throw new MiniTemplator.TemplateSyntaxException("Extra parameter in $EndBlock command in template at offset " + cmdTPosBegin + "."); }
        int blockNo = lookupBlockName(blockName);
        if (blockNo == -1) {
            throw new MiniTemplator.TemplateSyntaxException("Undefined block name \"" + blockName + "\" in $EndBlock command in template at offset " + cmdTPosBegin + "."); }
        currentNestingLevel--;
        BlockTabRec btr = blockTab[blockNo];
        if (!btr.definitionIsOpen) {
            throw new MiniTemplator.TemplateSyntaxException("Multiple $EndBlock command for block \"" + blockName + "\" in template at offset " + cmdTPosBegin + "."); }
        if (btr.nestingLevel != currentNestingLevel) {
            throw new MiniTemplator.TemplateSyntaxException("Block nesting level mismatch at $EndBlock command for block \"" + blockName + "\" in template at offset " + cmdTPosBegin + "."); }
        btr.tPosContentsEnd = cmdTPosBegin;
        btr.tPosEnd = cmdTPosEnd;
        btr.definitionIsOpen = false; }

    // Returns the block number of the newly registered block.
    private int registerBlock (String blockName) {
        int blockNo = blockTabCnt++;
        if (blockTabCnt > blockTab.length) {
            blockTab = (BlockTabRec[])resizeArray(blockTab, 2*blockTabCnt); }
        BlockTabRec btr = new BlockTabRec();
        blockTab[blockNo] = btr;
        btr.blockName = blockName;
        if (blockName != null) {
            btr.nextWithSameName = lookupBlockName(blockName); }
        else {
            btr.nextWithSameName = -1; }
        btr.nestingLevel = currentNestingLevel;
        if (currentNestingLevel > 0) {
            btr.parentBlockNo = openBlocksTab[currentNestingLevel-1]; }
        else {
            btr.parentBlockNo = -1; }
        btr.definitionIsOpen = true;
        btr.blockVarCnt = 0;
        btr.firstVarRefNo = -1;
        btr.blockVarNoToVarNoMap = new int[32];
        btr.dummy = false;
        if (blockName != null) {
            blockNameToNoMap.put(blockName.toUpperCase(), new Integer(blockNo)); }
        return blockNo; }

    // Registers a dummy block to exclude a range within the template text.
    private void excludeTemplateRange (int tPosBegin, int tPosEnd) {
        if (blockTabCnt > 0) {
            // Check whether we can extend the previous block.
            BlockTabRec btr = blockTab[blockTabCnt-1];
            if (btr.dummy && btr.tPosEnd == tPosBegin) {
                btr.tPosContentsEnd = tPosEnd;
                btr.tPosEnd = tPosEnd;
                return; }}
        int blockNo = registerBlock(null);
        BlockTabRec btr = blockTab[blockNo];
        btr.tPosBegin = tPosBegin;
        btr.tPosContentsBegin = tPosBegin;
        btr.tPosContentsEnd = tPosEnd;
        btr.tPosEnd = tPosEnd;
        btr.definitionIsOpen = false;
        btr.dummy = true; }

    // Checks that all block definitions are closed.
    private void checkBlockDefinitionsComplete()
            throws MiniTemplator.TemplateSyntaxException {
        for (int blockNo=0; blockNo<blockTabCnt; blockNo++) {
            BlockTabRec btr = blockTab[blockNo];
            if (btr.definitionIsOpen) {
                throw new MiniTemplator.TemplateSyntaxException("Missing $EndBlock command in template for block \"" + btr.blockName + "\"."); }}
        if (currentNestingLevel != 0) {
            throw new MiniTemplator.TemplateSyntaxException("Block nesting level error at end of template."); }}

    // Processes the $include command.
    private void processIncludeCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        if (conditionalExclude(cmdTPosBegin, cmdTPosEnd)) {
            return; }
        int p0 = skipBlanks(parms, 0);
        if (p0 >= parms.length()) {
            throw new MiniTemplator.TemplateSyntaxException("Missing subtemplate name in $Include command in template at offset " + cmdTPosBegin + "."); }
        int p;
        if (parms.charAt(p0) == '"') {                          // subtemplate name is quoted
            p0++;
            p = parms.indexOf('"', p0);
            if (p == -1) {
                throw new MiniTemplator.TemplateSyntaxException("Missing closing quote for subtemplate name in $Include command in template at offset " + cmdTPosBegin + "."); }}
        else {
            p = skipNonBlanks(parms, p0); }
        String subtemplateName = parms.substring(p0, p);
        p++;
        if (!isRestOfStringBlank(parms, p)) {
            throw new MiniTemplator.TemplateSyntaxException("Extra parameter in $Include command in template at offset " + cmdTPosBegin + "."); }
        insertSubtemplate(subtemplateName, cmdTPosBegin, cmdTPosEnd); }

    private void insertSubtemplate (String subtemplateName, int tPos1, int tPos2) {
        if (templateText.length() > maxInclTemplateSize) {
            throw new RuntimeException("Subtemplate include aborted because the internal template string is longer than "+maxInclTemplateSize+" characters."); }
        String subtemplate;
        try {
            subtemplate = miniTemplator.loadSubtemplate(subtemplateName); }
        catch (IOException e) {
            throw new RuntimeException("Error while loading subtemplate \""+subtemplateName+"\"", e); }
        // (Copying the template to insert a subtemplate is a bit slow. In a future implementation of MiniTemplator,
        // a table could be used that contains references to the string fragments.)
        StringBuilder s = new StringBuilder(templateText.length()+subtemplate.length());
        s.append(templateText, 0, tPos1);
        s.append(subtemplate);
        s.append(templateText, tPos2, templateText.length());
        templateText = s.toString();
        resumeCmdParsingFromStart = true; }

//--- Conditional commands -----------------------------------------------------

    // Returns the enabled/disabled state of the condition at level condLevel2.
    private boolean isCondEnabled (int condLevel2) {
        if (condLevel2 < 0) {
            return true; }
        return condEnabled[condLevel2]; }

    // If the current condition is disabled, the text from tPosBegin to tPosEnd
// is excluded and true is returned.
// Otherwise nothing is done and false is returned.
    private boolean conditionalExclude (int tPosBegin, int tPosEnd) {
        if (isCondEnabled(condLevel)) {
            return false; }
        excludeTemplateRange(tPosBegin, tPosEnd);
        return true; }

    // Evaluates a condition expression of a conditional command, by comparing the
// flags in the expression with the flags in TemplateSpecification.conditionFlags.
// Returns true the condition is met.
    private boolean evaluateConditionFlags (String flags) {
        int p = 0;
        while (true) {
            p = skipBlanks(flags, p);
            if (p >= flags.length()) {
                break; }
            boolean complement = false;
            if (flags.charAt(p) == '!') {
                complement = true; p++; }
            p = skipBlanks(flags, p);
            if (p >= flags.length()) {
                break; }
            int p0 = p;
            p = skipNonBlanks(flags, p0+1);
            String flag = flags.substring(p0, p).toUpperCase();
            if ((conditionFlags != null && conditionFlags.contains(flag)) ^ complement) {
                return true; }}
        return false; }

    // Processes the $if command.
    private void processIfCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        excludeTemplateRange(cmdTPosBegin, cmdTPosEnd);
        if (condLevel >= maxCondLevels-1) {
            throw new MiniTemplator.TemplateSyntaxException ("Too many nested $if commands."); }
        condLevel++;
        boolean enabled = isCondEnabled(condLevel-1) && evaluateConditionFlags(parms);
        condEnabled[condLevel] = enabled;
        condPassed[condLevel] = enabled; }

    // Processes the $elseIf command.
    private void processElseIfCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        excludeTemplateRange(cmdTPosBegin, cmdTPosEnd);
        if (condLevel < 0) {
            throw new MiniTemplator.TemplateSyntaxException ("$elseIf without matching $if."); }
        boolean enabled = isCondEnabled(condLevel-1) && !condPassed[condLevel] && evaluateConditionFlags(parms);
        condEnabled[condLevel] = enabled;
        if (enabled) {
            condPassed[condLevel] = true; }}

    // Processes the $else command.
    private void processElseCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        excludeTemplateRange(cmdTPosBegin, cmdTPosEnd);
        if (parms.trim().length() != 0) {
            throw new MiniTemplator.TemplateSyntaxException ("Invalid parameters for $else command."); }
        if (condLevel < 0) {
            throw new MiniTemplator.TemplateSyntaxException ("$else without matching $if."); }
        boolean enabled = isCondEnabled(condLevel-1) && !condPassed[condLevel];
        condEnabled[condLevel] = enabled;
        if (enabled) {
            condPassed[condLevel] = true; }}

    // Processes the $endIf command.
    private void processEndIfCmd (String parms, int cmdTPosBegin, int cmdTPosEnd)
            throws MiniTemplator.TemplateSyntaxException {
        excludeTemplateRange(cmdTPosBegin, cmdTPosEnd);
        if (parms.trim().length() != 0) {
            throw new MiniTemplator.TemplateSyntaxException ("Invalid parameters for $endIf command."); }
        if (condLevel < 0) {
            throw new MiniTemplator.TemplateSyntaxException ("$endif without matching $if."); }
        condLevel--; }

//------------------------------------------------------------------------------

    // Associates variable references with blocks.
    private void associateVariablesWithBlocks() {
        int varRefNo = 0;
        int activeBlockNo = 0;
        int nextBlockNo = 1;
        while (varRefNo < varRefTabCnt) {
            VarRefTabRec vrtr = varRefTab[varRefNo];
            int varRefTPos = vrtr.tPosBegin;
            int varNo = vrtr.varNo;
            if (varRefTPos >= blockTab[activeBlockNo].tPosEnd) {
                activeBlockNo = blockTab[activeBlockNo].parentBlockNo;
                continue; }
            if (nextBlockNo < blockTabCnt && varRefTPos >= blockTab[nextBlockNo].tPosBegin) {
                activeBlockNo = nextBlockNo;
                nextBlockNo++;
                continue; }
            BlockTabRec btr = blockTab[activeBlockNo];
            if (varRefTPos < btr.tPosBegin) {
                throw new AssertionError(); }
            int blockVarNo = btr.blockVarCnt++;
            if (btr.blockVarCnt > btr.blockVarNoToVarNoMap.length) {
                btr.blockVarNoToVarNoMap = (int[])resizeArray(btr.blockVarNoToVarNoMap, 2*btr.blockVarCnt); }
            btr.blockVarNoToVarNoMap[blockVarNo] = varNo;
            if (btr.firstVarRefNo == -1) {
                btr.firstVarRefNo = varRefNo; }
            vrtr.blockNo = activeBlockNo;
            vrtr.blockVarNo = blockVarNo;
            varRefNo++; }}

    // Parses variable references within the template in the format "${VarName}" .
    private void parseTemplateVariables()
            throws MiniTemplator.TemplateSyntaxException {
        int p = 0;
        while (true) {
            p = templateText.indexOf("${", p);
            if (p == -1) {
                break; }
            int p0 = p;
            p = templateText.indexOf("}", p);
            if (p == -1) {
                throw new MiniTemplator.TemplateSyntaxException("Invalid variable reference in template at offset " + p0 + "."); }
            p++;
            String varName = templateText.substring(p0+2, p-1).trim();
            if (varName.length() == 0) {
                throw new MiniTemplator.TemplateSyntaxException("Empty variable name in template at offset " + p0 + "."); }
            registerVariableReference(varName, p0, p); }}

    private void registerVariableReference (String varName, int tPosBegin, int tPosEnd) {
        int varNo;
        varNo = lookupVariableName(varName);
        if (varNo == -1) {
            varNo = registerVariable(varName); }
        int varRefNo = varRefTabCnt++;
        if (varRefTabCnt > varRefTab.length) {
            varRefTab = (VarRefTabRec[])resizeArray(varRefTab, 2*varRefTabCnt); }
        VarRefTabRec vrtr = new VarRefTabRec();
        varRefTab[varRefNo] = vrtr;
        vrtr.tPosBegin = tPosBegin;
        vrtr.tPosEnd = tPosEnd;
        vrtr.varNo = varNo; }

    // Returns the variable number of the newly registered variable.
    private int registerVariable (String varName) {
        int varNo = varTabCnt++;
        if (varTabCnt > varTab.length) {
            varTab = (String[])resizeArray(varTab, 2*varTabCnt); }
        varTab[varNo] = varName;
        varNameToNoMap.put(varName.toUpperCase(), new Integer(varNo));
        return varNo; }

//--- name lookup routines -------------------------------------------

    // Maps variable name to variable number.
// Returns -1 if the variable name is not found.
    public int lookupVariableName (String varName) {
        Integer varNoWrapper = varNameToNoMap.get(varName.toUpperCase());
        if (varNoWrapper == null) {
            return -1; }
        int varNo = varNoWrapper.intValue();
        return varNo; }

    // Maps block name to block number.
// If there are multiple blocks with the same name, the block number of the last
// registered block with that name is returned.
// Returns -1 if the block name is not found.
    public int lookupBlockName (String blockName) {
        Integer blockNoWrapper = blockNameToNoMap.get(blockName.toUpperCase());
        if (blockNoWrapper == null) {
            return -1; }
        int blockNo = blockNoWrapper.intValue();
        return blockNo; }

//--- general utility routines ---------------------------------------

    // Reallocates an array with a new size and copies the contents
// of the old array to the new array.
    public static Object resizeArray (Object oldArray, int newSize) {
        int oldSize = java.lang.reflect.Array.getLength(oldArray);
        Class<?> elementType = oldArray.getClass().getComponentType();
        Object newArray = java.lang.reflect.Array.newInstance(
                elementType, newSize);
        int preserveLength = Math.min(oldSize, newSize);
        if (preserveLength > 0) {
            System.arraycopy(oldArray, 0, newArray, 0, preserveLength); }
        return newArray; }

    // Skips blanks (white space) in string s starting at position p.
    private static int skipBlanks (String s, int p) {
        while (p < s.length() && Character.isWhitespace(s.charAt(p))) p++;
        return p; }

    // Skips non-blanks (no-white space) in string s starting at position p.
    private static int skipNonBlanks (String s, int p) {
        while (p < s.length() && !Character.isWhitespace(s.charAt(p))) p++;
        return p; }

    // Returns true if string s is blank (white space) from position p to the end.
    public static boolean isRestOfStringBlank (String s, int p) {
        return skipBlanks(s, p) >= s.length(); }

}
