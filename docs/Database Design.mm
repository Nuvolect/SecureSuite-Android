<map version="1.0.1">
<!-- To view this file, download free mind mapping software FreeMind from http://freemind.sourceforge.net -->
<node CREATED="1399645184316" ID="ID_1412069834" MODIFIED="1399645201458" TEXT="Data Design">
<node CREATED="1399645201485" ID="ID_654367980" MODIFIED="1399645362062" POSITION="right" TEXT="SQLCipher">
<node CREATED="1410374120916" ID="ID_1449661306" MODIFIED="1410374434981" TEXT="detail_db">
<node CREATED="1410374441250" ID="ID_1250928125" MODIFIED="1410374448247" TEXT="detail_table">
<node CREATED="1410374153811" ID="ID_99562125" MODIFIED="1410374161091" TEXT="_id"/>
<node CREATED="1410374149952" ID="ID_1926560328" MODIFIED="1410374165895" TEXT="contact_id"/>
<node CREATED="1399645475511" ID="ID_302820938" MODIFIED="1410374144652" TEXT="display_name"/>
<node CREATED="1410374216631" ID="ID_1759604811" MODIFIED="1410374220636" TEXT="kv">
<node CREATED="1410374241382" ID="ID_1178857821" MODIFIED="1410374262588" TEXT="company, name_first, name_last, name_middle, name_prefix, name_suffix, nickname, note, organization, phonetic_family, phonetic_given, phonetic_middle, title"/>
</node>
<node CREATED="1410374274963" ID="ID_542662114" MODIFIED="1410374278609" TEXT="photo">
<node CREATED="1410374278610" ID="ID_647163792" MODIFIED="1410374288147" TEXT="base64 encoded"/>
</node>
<node CREATED="1399645555509" ID="ID_766113687" MODIFIED="1410374294834" TEXT="ArrayList">
<node CREATED="1399645558805" ID="ID_142443856" MODIFIED="1399645563889" TEXT="Email">
<node CREATED="1399645563889" ID="ID_974886930" MODIFIED="1399645565971" TEXT="Key is type">
<node CREATED="1399645565972" ID="ID_486700577" MODIFIED="1399645584869" TEXT="Email is value"/>
</node>
</node>
</node>
<node CREATED="1399645500372" ID="ID_103760539" MODIFIED="1399645502960" TEXT="ArrayList">
<node CREATED="1399645502961" ID="ID_194183945" MODIFIED="1399645538892" TEXT="Phone">
<node CREATED="1399645538894" ID="ID_128412071" MODIFIED="1399645547194" TEXT="Key is type">
<node CREATED="1399645547195" ID="ID_1813573485" MODIFIED="1399645550388" TEXT="Number is value"/>
</node>
</node>
</node>
<node CREATED="1399645587615" ID="ID_1773613511" MODIFIED="1399645593951" TEXT="ArrayList">
<node CREATED="1399645593952" ID="ID_1189115372" MODIFIED="1399645596233" TEXT="Address">
<node CREATED="1399645596234" ID="ID_24180138" MODIFIED="1399645598776" TEXT="Key is type">
<node CREATED="1399645598777" ID="ID_816329909" MODIFIED="1399645611555" TEXT="Address is value">
<node CREATED="1399645766875" ID="ID_1390395320" MODIFIED="1399645777468" TEXT="String with newline"/>
</node>
</node>
</node>
</node>
<node CREATED="1399648178545" ID="ID_1188093850" MODIFIED="1410374343130" TEXT="ArrayList">
<node CREATED="1399648182180" ID="ID_619575505" MODIFIED="1399648192078" TEXT="Website">
<node CREATED="1399648192079" ID="ID_559354473" MODIFIED="1399648194553" TEXT="Key is type">
<node CREATED="1399648194553" ID="ID_1283323033" MODIFIED="1399648200582" TEXT="Website is value"/>
</node>
</node>
</node>
<node CREATED="1410374317275" ID="ID_1585067930" MODIFIED="1410374320570" TEXT="ArralList">
<node CREATED="1410374320571" ID="ID_1061470711" MODIFIED="1410374323461" TEXT="IM"/>
</node>
<node CREATED="1410374344117" ID="ID_1120966395" MODIFIED="1410374361085" TEXT="ArrayList">
<node CREATED="1410374346660" ID="ID_907170609" MODIFIED="1410374353611" TEXT="Dates"/>
</node>
<node CREATED="1410374364592" ID="ID_998096133" MODIFIED="1410374367912" TEXT="ArrayList">
<node CREATED="1410374367912" ID="ID_973195504" MODIFIED="1410374371262" TEXT="Relation"/>
</node>
<node CREATED="1410374378047" ID="ID_1613757619" MODIFIED="1410374457599" TEXT="ArrayList">
<node CREATED="1410374382853" ID="ID_1014683597" MODIFIED="1410374385989" TEXT="Internetcall"/>
</node>
</node>
</node>
<node CREATED="1410374128847" ID="ID_195050440" MODIFIED="1410374132187" TEXT="account_db">
<node CREATED="1399645614234" ID="ID_1868428443" MODIFIED="1399645621063" TEXT="ArrayList">
<node CREATED="1399645621064" ID="ID_568069883" MODIFIED="1399645648002" TEXT="Group">
<node CREATED="1399645623291" ID="ID_1290212496" MODIFIED="1399645654732" TEXT="Value"/>
</node>
</node>
<node CREATED="1399645681992" ID="ID_1296917245" MODIFIED="1410374465331" TEXT="ArrayList">
<node CREATED="1399645688215" ID="ID_310108751" MODIFIED="1399645731347" TEXT="Account">
<node CREATED="1399645731347" ID="ID_650784562" MODIFIED="1399645736814" TEXT="Key is type">
<node CREATED="1399645736815" ID="ID_1149471305" MODIFIED="1399645759684" TEXT="Account is value">
<node CREATED="1399645841812" ID="ID_914042342" MODIFIED="1399645859318" TEXT="Account object">
<node CREATED="1399645859318" ID="ID_948809499" MODIFIED="1399645866605" TEXT="Login name"/>
<node CREATED="1399645867383" ID="ID_1757909690" MODIFIED="1399645871555" TEXT="Login password"/>
<node CREATED="1399645872322" ID="ID_1704633248" MODIFIED="1399645878745" TEXT="URL"/>
<node CREATED="1399645882684" ID="ID_704768565" MODIFIED="1399645889285" TEXT="Note"/>
</node>
</node>
</node>
</node>
</node>
<node CREATED="1410374467047" ID="ID_280602406" MODIFIED="1410374474605" TEXT="account_table">
<node CREATED="1410374500572" ID="ID_1312308732" MODIFIED="1410374502742" TEXT="_id"/>
<node CREATED="1410374504014" ID="ID_44770507" MODIFIED="1410374507074" TEXT="contact_id"/>
<node CREATED="1410374507581" ID="ID_946019331" MODIFIED="1410374510190" TEXT="display_name"/>
<node CREATED="1410374510899" ID="ID_1942248333" MODIFIED="1410374515095" TEXT="display_name_source"/>
<node CREATED="1410374516075" ID="ID_714607401" MODIFIED="1410374517806" TEXT="starred"/>
<node CREATED="1410374518505" ID="ID_1350826849" MODIFIED="1410374520900" TEXT="account_name"/>
<node CREATED="1410374522161" ID="ID_1353142887" MODIFIED="1410374525299" TEXT="account_type"/>
</node>
<node CREATED="1410374475092" ID="ID_1738572017" MODIFIED="1410374479466" TEXT="account_data_table">
<node CREATED="1410374531600" ID="ID_1253018772" MODIFIED="1410374533320" TEXT="_id"/>
<node CREATED="1410374533862" ID="ID_809069231" MODIFIED="1410374538192" TEXT="contact_id"/>
<node CREATED="1410374539610" ID="ID_990787982" MODIFIED="1410374541555" TEXT="group_id"/>
</node>
<node CREATED="1410374480287" ID="ID_148505729" MODIFIED="1410374484326" TEXT="group_title_table">
<node CREATED="1410374545200" ID="ID_1939468073" MODIFIED="1410374549891" TEXT="_id"/>
<node CREATED="1410374550488" ID="ID_1762894298" MODIFIED="1410374552704" TEXT="group_id"/>
<node CREATED="1410374553110" ID="ID_1223967382" MODIFIED="1410374557171" TEXT="title"/>
<node CREATED="1410374557926" ID="ID_1198810565" MODIFIED="1410374560197" TEXT="account_name"/>
<node CREATED="1410374561570" ID="ID_561474205" MODIFIED="1410374564562" TEXT="account_type"/>
<node CREATED="1410374565396" ID="ID_373157716" MODIFIED="1410374568848" TEXT="group_visible"/>
</node>
<node CREATED="1410374488118" ID="ID_386806676" MODIFIED="1410374492820" TEXT="account_cryp_table">
<node CREATED="1410374576105" ID="ID_773452494" MODIFIED="1410374578241" TEXT="_id"/>
<node CREATED="1410374578716" ID="ID_639059908" MODIFIED="1410374582281" TEXT="key"/>
<node CREATED="1410374583272" ID="ID_41824082" MODIFIED="1410374584541" TEXT="value"/>
</node>
</node>
<node CREATED="1401127565530" ID="ID_207180572" MODIFIED="1410374019021" TEXT="Export to vCard"/>
<node CREATED="1401127576386" ID="ID_1314045013" MODIFIED="1410374052413" TEXT="Import from vCard">
<node CREATED="1401128490195" ID="ID_514968694" MODIFIED="1401128498079" TEXT="Import JSON contact"/>
</node>
<node CREATED="1401127582990" ID="ID_1430509565" MODIFIED="1401127591470" TEXT="Backup encrypted"/>
<node CREATED="1401127592080" ID="ID_1043527104" MODIFIED="1401127595982" TEXT="Restore encrypted"/>
</node>
<node CREATED="1401127511081" ID="ID_1947198537" MODIFIED="1401127525278" POSITION="right" TEXT="Android Contact DB">
<node CREATED="1401127617045" ID="ID_1194126478" MODIFIED="1401127627959" TEXT="Dump to log"/>
<node CREATED="1401127525904" ID="ID_541543822" MODIFIED="1401127539472" TEXT="Import"/>
<node CREATED="1401127539947" ID="ID_869626264" MODIFIED="1401130189447" TEXT="Import with merge by Name">
<node CREATED="1401136924840" ID="ID_1198029040" MODIFIED="1401136929928" TEXT="Init Import"/>
<node CREATED="1401128409328" ID="ID_313580423" MODIFIED="1401128412623" TEXT="Get JSON contact"/>
<node CREATED="1401136919596" ID="ID_900741053" MODIFIED="1401136922678" TEXT="Close Import"/>
<node CREATED="1401128413243" ID="ID_1732894290" MODIFIED="1401128440186" TEXT="Merge JSON contact">
<node CREATED="1401128617681" ID="ID_197233032" MODIFIED="1401128651692" TEXT="If name match">
<node CREATED="1401128651693" ID="ID_1002810919" MODIFIED="1401128670333" TEXT="Walk each element">
<node CREATED="1401128686184" ID="ID_1836487029" MODIFIED="1401128687038" TEXT="1">
<node CREATED="1401128713139" ID="ID_91709674" MODIFIED="1401128730420" TEXT="Hash each sub-element">
<node CREATED="1401128730421" ID="ID_816534461" MODIFIED="1401128744146" TEXT="Presidence rule">
<node CREATED="1401128744147" ID="ID_976101488" MODIFIED="1401128751198" TEXT="Import over existing"/>
<node CREATED="1401128751750" ID="ID_217562338" MODIFIED="1401128755248" TEXT="Existing over import"/>
</node>
<node CREATED="1401128778109" ID="ID_302590802" MODIFIED="1401128812818" TEXT="Duplicate key (home) is OK"/>
<node CREATED="1401128815021" ID="ID_1522832415" MODIFIED="1401128869552" TEXT="Duplicate key and value is deleted"/>
<node CREATED="1401128876132" ID="ID_1457406952" MODIFIED="1401128899205" TEXT="Duplicate value is OK">
<node CREATED="1401130259514" ID="ID_1988290112" MODIFIED="1401130268783" TEXT="option: keep 1"/>
</node>
</node>
</node>
<node CREATED="1401128687490" ID="ID_628183797" MODIFIED="1401128687870" TEXT="2">
<node CREATED="1401128910850" ID="ID_290238968" MODIFIED="1401128916846" TEXT="Replace sub-element"/>
</node>
</node>
</node>
</node>
</node>
</node>
<node CREATED="1401136665555" ID="ID_1609319971" MODIFIED="1401136674668" POSITION="right" TEXT="DummyContactData">
<node CREATED="1401136859229" ID="ID_1982484345" MODIFIED="1401136898640" TEXT="Init Import"/>
<node CREATED="1401136674669" ID="ID_1020437921" MODIFIED="1401136682081" TEXT="Get JSON contact"/>
<node CREATED="1401136890481" ID="ID_447585590" MODIFIED="1401136893911" TEXT="Close Import"/>
</node>
</node>
</map>