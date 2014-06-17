/*
 * Constants.java
 *
 * Created on October 29, 2006, 11:57 AM
 *
 * Description: Contains the constant values for the kb package.
 *
 * Copyright (C) 2006 Stephen L. Reed.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.texai.kb;

/** This class contains the static constants used by the semantic annotation persistence framework.
 *
 * @author reed
 */
public class Constants {

  // terms
  /** the Abbreviation URI string */
  public static final String TERM_ABBREVIATION = "http://sw.cyc.com/2006/07/27/cyc/Abbreviation";
  /** the Acronym URI string */
  public static final String TERM_ACRONYM = "http://sw.cyc.com/2006/07/27/cyc/Acronym";
  /** the Adjective URI string */
  public static final String TERM_ADJECTIVE = "http://sw.cyc.com/2006/07/27/cyc/Adjective";
  /** the AdjectivePhrase URI string */
  public static final String TERM_ADJECTIVE_PHRASE = "http://texai.org/texai/AdjectivePhrase";
  /** the Adverb URI string */
  public static final String TERM_ADVERB = "http://sw.cyc.com/2006/07/27/cyc/Adverb";
  /** the AdverbPhrase URI string */
  public static final String TERM_ADVERB_PHRASE = "http://texai.org/texai/AdverbPhrase";
  /** the arg1Isa URI string */
  public static final String TERM_ARG1_ISA = "http://sw.cyc.com/2006/07/27/cyc/arg1Isa";
  /** the arg2Isa URI string */
  public static final String TERM_ARG2_ISA = "http://sw.cyc.com/2006/07/27/cyc/arg2Isa";
  /** the arg2Isa URI string */
  public static final String TERM_ARG3_ISA = "http://sw.cyc.com/2006/07/27/cyc/arg3Isa";
  /** the arg2Isa URI string */
  public static final String TERM_ARG4_ISA = "http://sw.cyc.com/2006/07/27/cyc/arg4Isa";
  /** the arg2Isa URI string */
  public static final String TERM_ARG5_ISA = "http://sw.cyc.com/2006/07/27/cyc/arg5Isa";
  /** the arg2Isa URI string */
  public static final String TERM_ARITY = "http://sw.cyc.com/2006/07/27/cyc/arity";
  /** the AuxVerb URI string */
  public static final String TERM_AUX_VERB = "http://sw.cyc.com/2006/07/27/cyc/AuxVerb";
  /** the base knowledge base context URI string */
  public static final String TERM_BASE_KB = "http://sw.cyc.com/2006/07/27/cyc/BaseKB";
  /** the BinaryPredicate URI string */
  public static final String TERM_BINARY_PREDICATE = "http://sw.cyc.com/2006/07/27/cyc/BinaryPredicate";
  /** the CharacterString URI string */
  public static final String TERM_CHARACTER_STRING = "http://sw.cyc.com/2006/07/27/cyc/CharacterString";
  /** the CMUPronouncingDictionaryContext URI string */
  public static final String TERM_CMU_PRONOUNCING_DICTIONARY_CONTEXT = "http://texai.org/texai/CMUPronouncingDictionaryContext";
  /** the cmuWordForm URI string */
  public static final String TERM_CMU_WORD_FORM = "http://texai.org/texai/cmuWordForm";
  /** the Collection URI string */
  public static final String TERM_COLLECTION = "http://sw.cyc.com/2006/07/27/cyc/Collection";
  /** the CollectiveNoun URI string */
  public static final String TERM_COLLECTIVE_NOUN = "http://texai.org/texai/CollectiveNoun";
  /** the ComputerDataStructure URI string */
  public static final String TERM_COMPUTER_DATA_STRUCTURE = "http://sw.cyc.com/2006/07/27/cyc/ComputerDataStructure";
  /** the ComparativeAdjective URI string */
  public static final String TERM_COMPARATIVE_ADJECTIVE = "http://texai.org/texai/ComparativeAdjective";
  /** the conceptuallyRelated URI string */
  public static final String TERM_CONCEPTUALLY_RELATED = "http://sw.cyc.com/2006/07/27/cyc/conceptuallyRelated";
  /** the conceptuallyUnrelated URI string */
  public static final String TERM_CONCEPTUALLY_UNRELATED = "http://texai.org/texai/conceptuallyUnrelated";
  /** the Conjunction URI string */
  public static final String TERM_CONJUNCTION = "http://sw.cyc.com/2006/07/27/cyc/Conjunction";
  /** the Contraction-NLPhrase URI string */
  public static final String TERM_CONTRACTION_NL_PHRASE = "http://sw.cyc.com/2006/07/27/cyc/Contraction-NLPhrase";
  /** the ControlCharacterFreeString URI string */
  public static final String TERM_CONTROL_CHARACTER_FREE_STRING = "http://sw.cyc.com/2006/07/27/cyc/ControlCharacterFreeString";
  /** the Conversation URI string */
  public static final String TERM_CONVERSATION = "http://sw.cyc.com/2006/07/27/cyc/Conversation";
  /** the CountNoun URI string */
  public static final String TERM_COUNT_NOUN = "http://sw.cyc.com/2006/07/27/cyc/CountNoun";
  /** the CxgComposedConstruction URI string */
  public static final String TERM_CXG_COMPOSED_CONSTRUCTION = "http://texai.org/texai/CxgComposedConstruction";
  /** the cxgConstituentAdapterName URI string */
  public static final String TERM_CXG_CONSTITUENT_ADAPTER_NAME = "http://texai.org/texai/cxgConstituentAdapterName";
  /** the CxgConstruction URI string */
  public static final String TERM_CXG_CONSTRUCTION = "http://texai.org/texai/CxgConstruction";
  /** the cxgConstructionName URI string */
  public static final String TERM_CXG_CONSTRUCTION_NAME = "http://texai.org/texai/cxgConstructionName";
  /** the cxgPhonemeName URI string */
  public static final String TERM_CXG_PHONEME_NAME = "http://texai.org/texai/cxgPhonemeName";
  /** the cxgProposition URI string */
  public static final String TERM_CXG_PROPOSITION = "http://texai.org/texai/cxgProposition";
  /** the http://sw.cyc.com/2006/07/27/cyc/ URI string */
  public static final String TERM_CXG_TEXAI_ENGLISH_WORD_FORM = "http://texai.org/texai/cxgTexaiEnglishWordForm";
  /** the texai:cxgWordForm URI string */
  public static final String TERM_CXG_WORD_FORM = "http://texai.org/texai/cxgWordForm";
  /** the CycAgent URI string */
  public static final String TERM_CYC_AGENT = "http://sw.cyc.com/2006/07/27/cyc/CycAgent";
  /** the deliberateActors URI string */
  public static final String TERM_DELIBERATE_ACTORS = "http://sw.cyc.com/2006/07/27/cyc/deliberateActors";
  /** the DemonstrativePronoun URI string */
  public static final String TERM_DEMONSTRATIVE_PRONOUN = "http://texai.org/texai/DemonstrativePronoun";
  /** the Determiner URI string */
  public static final String TERM_DETERMINER = "http://sw.cyc.com/2006/07/27/cyc/Determiner";
  /** the Determiner-Definite URI string */
  public static final String TERM_DETERMINER_DEFINITE = "http://sw.cyc.com/2006/07/27/cyc/Determiner-Definite";
  /** the Determiner-Indefinite URI string */
  public static final String TERM_DETERMINER_INDEFINITE = "http://sw.cyc.com/2006/07/27/cyc/Determiner-Indefinite";
  /** the DialogUser URI string */
  public static final String TERM_DIALOG_USER = "http://texai.org/texai/DialogUser";
  /** the domainEntityClassName context URI string */
  public static final String TERM_DOMAIN_ENTITY_CLASS_NAME = "http://texai.org/texai/domainEntityClassName";
  /** the EnglishConstructionGrammarDomainContext URI string */
  public static final String TERM_ENGLISH_CONSTRUCTION_GRAMMAR_DOMAIN_CONTEXT = "http://texai.org/texai/EnglishConstructionGrammarDomainContext";
  /** the EnglishLanguage URI string */
  public static final String TERM_ENGLISH_LANGUAGE = "http://sw.cyc.com/2006/07/27/cyc/EnglishLanguage";
  /** the EnglishMt URI string */
  public static final String TERM_ENGLISH_MT = "http://sw.cyc.com/2006/07/27/cyc/EnglishMt";
  /** the EnglishWord URI string */
  public static final String TERM_ENGLISH_WORD = "http://sw.cyc.com/2006/07/27/cyc/EnglishWord";
  /** the EverythingPSC context URI string */
  public static final String TERM_EVERYTHING_PSC = "http://texai.org/texai/EverythingPSC";
  /** the equals URI string */
  public static final String TERM_EQUALS = "http://sw.cyc.com/2006/07/27/cyc/equals";
  /** the FirstOrderCollection context URI string */
  public static final String TERM_FIRST_ORDER_COLLECTION = "http://sw.cyc.com/2006/07/27/cyc/FirstOrderCollection";
  /** the FunctionalSlot URI string */
  public static final String TERM_FUNCTIONAL_SLOT = "http://sw.cyc.com/2006/07/27/cyc/FunctionalSlot";
  /** the genlMt URI string */
  public static final String TERM_GENL_MT = "http://sw.cyc.com/2006/07/27/cyc/genlMt";
  /** the GerundiveNoun URI string */
  public static final String TERM_GERUNDIVE_NOUN = "http://sw.cyc.com/2006/07/27/cyc/GerundiveNoun";
  /** the IndefinitePronoun URI string */
  public static final String TERM_INDEFINITE_PRONOUN = "http://sw.cyc.com/2006/07/27/cyc/IndefinitePronoun";
  /** the IdiomPhrase URI string */
  public static final String TERM_IDIOM_PHRASE = "http://texai.org/texai/IdiomPhrase";
  /** the Individual URI string */
  public static final String TERM_INDIVIDUAL = "http://sw.cyc.com/2006/07/27/cyc/Individual";
  /** the InfinitiveVerbWordForm URI string */
  public static final String TERM_INFINITIVE_VERB_WORD_FORM = "http://texai.org/texai/InfinitiveVerbWordForm";
  /** the initialism URI string */
  public static final String TERM_INITIALISM = "http://sw.cyc.com/2006/07/27/cyc/Initialism";
  /** the initiator URI string */
  public static final String TERM_INITIATOR = "http://sw.cyc.com/2006/07/27/cyc/initiator";
  /** the Integer URI string */
  public static final String TERM_INTEGER = "http://sw.cyc.com/2006/07/27/cyc/Integer";
  /** the IntelligentAgent URI string */
  public static final String TERM_INTELLIGENT_AGENT = "http://sw.cyc.com/2006/07/27/cyc/IntelligentAgent";
  /** the Interjection-SpeechPart URI string */
  public static final String TERM_INTERJECTION_SPEECH_PART = "http://sw.cyc.com/2006/07/27/cyc/Interjection-SpeechPart";
  /** the Interjection-InterjectionPhrase URI string */
  public static final String TERM_INTERJECTION_PHRASE = "http://texai.org/texai/InterjectionPhrase";
  /** the IntransitiveVerb URI string */
  public static final String TERM_INTRANSITIVE_VERB = "http://texai.org/texai/IntransitiveVerb";
  /** the LexicalItem URI string */
  public static final String TERM_LEXICAL_ITEM = "http://sw.cyc.com/2006/07/27/cyc/LexicalItem";
  /** the LinguisticObjectType URI string */
  public static final String TERM_LINGUISTIC_OBJECT_TYPE = "http://sw.cyc.com/2006/07/27/cyc/LinguisticObjectType";
  /** the MassNoun URI string */
  public static final String TERM_MASS_NOUN = "http://sw.cyc.com/2006/07/27/cyc/MassNoun";
  /** the Microtheory URI string */
  public static final String TERM_MICROTHEORY = "http://sw.cyc.com/2006/07/27/cyc/Microtheory";
  /** the NLPhrase URI string */
  public static final String TERM_NL_PHRASE = "http://sw.cyc.com/2006/07/27/cyc/NLPhrase";
  /** the NLPhraseType URI string */
  public static final String TERM_NL_PHRASE_TYPE = "http://sw.cyc.com/2006/07/27/cyc/NLPhraseType";
  /** the NLWordForm URI string */
  public static final String TERM_NL_WORD_FORM = "http://sw.cyc.com/2006/07/27/cyc/NLWordForm";
  /** the NonNegativeInteger URI string */
  public static final String TERM_NON_NEGATIVE_INTEGER = "http://sw.cyc.com/2006/07/27/cyc/NonNegativeInteger";
  /** the Noun URI string */
  public static final String TERM_NOUN = "http://sw.cyc.com/2006/07/27/cyc/Noun";
  /** the NounPhrase URI string */
  public static final String TERM_NOUN_PHRASE = "http://sw.cyc.com/2006/07/27/cyc/NounPhrase";
  /** the Now URI string */
  public static final String TERM_NOW = "http://sw.cyc.com/2006/07/27/cyc/Now";
  /** the ObjectType URI string */
  public static final String TERM_OBJECT_TYPE = "http://sw.cyc.com/2006/07/27/cyc/ObjectType";
  /** the On-SituationLocalized URI string */
  public static final String TERM_ON_SITUATION_LOCALIZED = "http://texai.org/texai/On-SituationLocalized";
  /** the overrideContext URI string */
  public static final String TERM_OVERRIDE_CONTEXT = "http://texai.org/texai/overrideContext";
  /** the owl:Thing URI string */
  public static final String TERM_OWL_THING = "http://www.w3.org/2002/07/owl#Thing";
  /** the Particle-SpeechPart URI string */
  public static final String TERM_PARTICLE_SPEECH_PART = "http://texai.org/texai/Particle-SpeechPart";
  /** the PastParticipleVerbWordForm URI string */
  public static final String TERM_PAST_PARTICIPLE_VERB_WORD_FORM = "http://texai.org/texai/PastParticipleVerbWordForm";
  /** the persistentMapEntry URI string */
  public static final String TERM_PERSISTENT_MAP_ENTRY_KEY = "http://texai.org/texai/persistentMapEntryKey";
  /** the persistentMapValue URI string */
  public static final String TERM_PERSISTENT_MAP_ENTRY_VALUE = "http://texai.org/texai/persistentMapEntryValue";
  /** the PluralWordForm URI string */
  public static final String TERM_PLURAL_NOUN_WORD_FORM = "http://texai.org/texai/PluralNounWordForm";
  /** the PositiveInteger URI string */
  public static final String TERM_POSITIVE_INTEGER = "http://sw.cyc.com/2006/07/27/cyc/PositiveInteger";
  /** the PossessivePronoun URI string */
  public static final String TERM_POSSESSIVE_PRONOUN = "http://sw.cyc.com/2006/07/27/cyc/PossessivePronoun";
  /** the PossessivePronoun-Pre URI string */
  public static final String TERM_POSSESSIVE_PRONOUN_PRE = "http://sw.cyc.com/2006/07/27/cyc/PossessivePronoun-Pre";
  /** the Preposition URI string */
  public static final String TERM_PREPOSITION = "http://sw.cyc.com/2006/07/27/cyc/Preposition";
  /** the PrepositionalPhrase URI string */
  public static final String TERM_PREPOSITIONAL_PHRASE = "http://sw.cyc.com/2006/07/27/cyc/PrepositionalPhrase";
  /** the PresentParticipleVerbWordForm URI string */
  public static final String TERM_PRESENT_PARTICIPLE_VERB_WORD_FORM = "http://texai.org/texai/PresentParticipleVerbWordForm";
  /** the prettyString-Canonical URI string */
  public static final String TERM_PRETTY_STRING_CANONICAL = "http://sw.cyc.com/2006/07/27/cyc/prettyString-Canonical";
  /** the Pronoun URI string */
  public static final String TERM_PRONOUN = "http://sw.cyc.com/2006/07/27/cyc/Pronoun";
  /** the PronounPhrase URI string */
  public static final String TERM_PRONOUN_PHRASE = "http://texai.org/texai/PronounPhrase";
  /** the properNameStrings URI string */
  public static final String TERM_PROPER_NAME_STRINGS = "http://sw.cyc.com/2006/07/27/cyc/properNameStrings";
  /** the ProperAdjective URI string */
  public static final String TERM_PROPER_ADJECTIVE = "http://texai.org/texai/ProperAdjective";
  /** the ProperNoun URI string */
  public static final String TERM_PROPER_NOUN = "http://sw.cyc.com/2006/07/27/cyc/ProperNoun";
  /** the properSubSituations URI string */
  public static final String TERM_PROPER_SUB_SITUATIONS = "http://sw.cyc.com/2006/07/27/cyc/properSubSituations";
  /** the rdf:List URI string */
  public static final String TERM_RDF_LIST = "http://www.w3.org/1999/02/22-rdf-syntax-ns#List";
  /** the ReflexiveVerb URI string */
  public static final String TERM_REFLEXIVE_VERB = "http://texai.org/texai/ReflexiveVerb";
  /** the RelativePronoun URI string */
  public static final String TERM_RELATIVE_PRONOUN = "http://texai.org/texai/RelativePronoun";
  /** the SingularNounWordForm URI string */
  public static final String TERM_SINGULAR_NOUN_WORD_FORM = "http://texai.org/texai/SingularNounWordForm";
  /** the SimplePastVerbWordForm URI string */
  public static final String TERM_SIMPLE_PAST_VERB_WORD_FORM = "http://texai.org/texai/SimplePastVerbWordForm";
  /** the Situation-Localized URI string */
  public static final String TERM_SITUATION_CONSTITUENTS = "http://sw.cyc.com/2006/07/27/cyc/situationConstituents";
  /** the situationHappeningAfterDate URI string */
  public static final String TERM_SITUATION_HAPPENING_AFTER_DATE = "http://texai.org/texai/situationHappeningAfterDate";
  /** the situationHappeningBeforeDate URI string */
  public static final String TERM_SITUATION_HAPPENING_BEFORE_DATE = "http://texai.org/texai/situationHappeningBeforeDate";
  /** the situationHappeningOnDate URI string */
  public static final String TERM_SITUATION_HAPPENING_ON_DATE = "http://texai.org/texai/situationHappeningOnDate";
  /** the situationConstituents URI string */
  public static final String TERM_SITUATION_LOCALIZED = "http://sw.cyc.com/2006/07/27/cyc/Situation-Localized";
  /** the SpeechPart URI string */
  public static final String TERM_SPEECH_PART = "http://sw.cyc.com/2006/07/27/cyc/SpeechPart";
  /** the startingDate URI string */
  public static final String TERM_STARTING_DATE = "http://sw.cyc.com/2006/07/27/cyc/startingDate";
  /** the SuperlativeAdjective URI string */
  public static final String TERM_SUPERLATIVE_ADJECTIVE = "http://texai.org/texai/SuperlativeAdjective";
  /** the TemporalStuffType URI string */
  public static final String TERM_TEMPORAL_STUFF_TYPE = "http://sw.cyc.com/2006/07/27/cyc/TemporalStuffType";
  /** the Texai URI string */
  public static final String TERM_TEXAI = "http://texai.org/texai/Texai";
  /** the TexaiEnglishLexiconContext URI string */
  public static final String TERM_TEXAI_ENGLISH_LEXICON_CONTEXT = "http://texai.org/texai/TexaiEnglishLexiconContext";
  /** the texaiEnglishWordFormForWord URI string */
  public static final String TERM_TEXAI_ENGLISH_WORD_FORM_FOR_WORD = "http://texai.org/texai/texaiEnglishWordFormForWord";
  /** the texaiEnglishWordSenseForWord URI string */
  public static final String TERM_TEXAI_ENGLISH_WORD_SENSE_FOR_WORD = "http://texai.org/texai/texaiEnglishWordSenseForWord";
  /** the texaiLemma URI string */
  public static final String TERM_TEXAI_LEMMA = "http://texai.org/texai/texaiLemma";
  /** the texaiWordNetEnglishWord URI string */
  public static final String TERM_TEXAI_WORD_NET_ENGLISH_WORD = "http://texai.org/texai/texaiWordNetEnglishWord";
  /** the texaiWordForm URI string */
  public static final String TERM_TEXAI_WORD_FORM = "http://texai.org/texai/texaiWordForm";
  /** the texaiWordSenseGloss URI string */
  public static final String TERM_TEXAI_WORD_SENSE_GLOSS = "http://texai.org/texai/texaiWordSenseGloss";
  /** the texaiWordSenseSamplePhrase URI string */
  public static final String TERM_TEXAI_WORD_SENSE_SAMPLE_PHRASE = "http://texai.org/texai/texaiWordSenseSamplePhrase";
  /** the Thing URI string */
  public static final String TERM_THING = "http://sw.cyc.com/2006/07/27/cyc/Thing";
  /** the ThirdPersonSingularVerbWordForm URI string */
  public static final String TERM_THIRD_PERSON_SINGULAR_VERB_WORD_FORM = "http://texai.org/texai/ThirdPersonSingularVerbWordForm";
  /** the TransitiveVerb URI string */
  public static final String TERM_TRANSITIVE_VERB = "http://texai.org/texai/TransitiveVerb";
  /** the UniversalVocabularyMt URI string */
  public static final String TERM_UNIVERSAL_VOCABULARY_MT = "http://sw.cyc.com/2006/07/27/cyc/UniversalVocabularyMt";
  /** the Verb URI string */
  public static final String TERM_VERB = "http://sw.cyc.com/2006/07/27/cyc/Verb";
  /** the VerbPhrase URI string */
  public static final String TERM_VERB_PHRASE = "http://sw.cyc.com/2006/07/27/cyc/VerbPhrase";
  /** the WHPronoun URI string */
  public static final String TERM_WH_PRONOUN = "http://sw.cyc.com/2006/07/27/cyc/WHPronoun";
  /** the WiktionaryContext URI string */
  public static final String TERM_WIKTIONARY_CONTEXT = "http://texai.org/texai/WiktionaryContext";
  /** the wiktionaryEnglishWordFormForWord URI string */
  public static final String TERM_WIKTIONARY_ENGLISH_WORD_FORM_FOR_WORD = "http://texai.org/texai/wiktionaryEnglishWordFormForWord";
  /** the wiktionaryEnglishWordSenseForWord URI string */
  public static final String TERM_WIKTIONARY_ENGLISH_WORD_SENSE_FOR_WORD = "http://texai.org/texai/wiktionaryEnglishWordSenseForWord";
  /** the WiktionaryInitializationProcess URI string */
  public static final String TERM_WIKTIONARY_INITIALIZATION_PROCESS = "http://texai.org/texai/WiktionaryInitializationProcess";
  /** the WiktionaryInitializationProject URI string */
  public static final String TERM_WIKTIONARY_INITIALIZATION_PROJECT = "http://texai.org/texai/WiktionaryInitializationProject";
  /** the wiktionaryLemma URI string */
  public static final String TERM_WIKTIONARY_LEMMA = "http://texai.org/texai/wiktionaryLemma";
  /** the wiktionaryWordSenseSamplePhrase URI string */
  public static final String TERM_WIKTIONARY_WORD_SENSE_SAMPLE_PHRASE = "http://texai.org/texai/wiktionaryWordSenseSamplePhrase";
  /** the wnLemma URI string */
  public static final String TERM_WN_LEMMA = "http://texai.org/texai/wnLemma";
  /** the WordNet21DomainContext URI string */
  public static final String TERM_WORDNET30 = "http://texai.org/texai/WordNet30";

  // namespaces
  /** the XML Schema namespace */
  public static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema#";
  /** the RDF namespace */
  public static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
  /** the RDF Schema namespace */
  public static final String RDFS_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";
  /** the OWL namespace */
  public static final String OWL_NAMESPACE = "http://www.w3.org/2002/07/owl#";
  /** the Cyc namespace */
  public static final String CYC_NAMESPACE = "http://sw.cyc.com/2006/07/27/cyc/";
  /** the Texai namespace */
  public static final String TEXAI_NAMESPACE = "http://texai.org/texai/";
  /** the Friend Of A Friend (FOAF) namespace */
  public static final String FOAF_NAMESPACE = "http://xmlns.com/foaf/0.1/";
  // Named caches
  /** the name of the cache for connected RDF entities, java entity --> RDF URI */
  public static final String CACHE_CONNECTED_RDF_ENTITIES = "connected RDF entities";
  /** the name of the cache for connected RDF entity URIs, entity hash --> entity id */
  public static final String CACHE_CONNECTED_RDF_ENTITY_URIS = "connected RDF entity URIs";
  /** the name of the cache for KB objects */
  public static final String CACHE_KB_OBJECTS = "KB objects";
  /** the name of the cache for metaphor object references */
  public static final String CACHE_METAPHOR_OBJECT_REFERENCES = "metaphor object references";
  /** the name of the cache for property restrictions */
  public static final String CACHE_PROPERTY_RESTRICTIONS = "property restrictions";
  /** the name of the cache for model situations */
  public static final String CACHE_SITUATION_MODELS = "situation models";
  /** the name of the cache for subject/property restrictions */
  public static final String CACHE_SUBJECT_PROPERTY_RESTRICTIONS = "subject/property restrictions";
  /** the name of the cache for verb complement model situations */
  public static final String CACHE_VERB_COMPLEMENT_SITUATION_MODELS = "verb complement situation models";
  // Repositories
  /** the name of the scripted behavior language repository */
  public static final String SCRIPTED_BEHAVIOR_LANGUAGE_REPOSITORY = "ScriptedBehaviorLanguage";
  // MapMessage constants
  /** the MapMessage key for the command value */
  public static final String MAP_MESSAGE_COMMAND = "command";
  // Miscellaneous
  /** the OpenCyc repository name */
  public static final String OPEN_CYC = "OpenCyc";
  /** the UTF-8 charset */
  public static final String UTF_8 = "UTF-8";
  /** the empty string */
  public static final String EMPTY_STRING = "";
  /** the length of the formula string column */
  public static final int FORMULA_STRING_SIZE = 4096;
  /** the java transaction manager name */
  public static final String JAVA_TRANSACTION_MANAGER_NAME = "java:/TransactionManager";
  /** the maximum length to return in the PClob.toString() method */
  public static final int MAX_LENGTH_AS_STRING = 100;
  /** the maximum executor thread pool size */
  public static final int MAX_THREAD_POOL_SIZE = 5;
  /** the message expiration time of five minutes */
  public static final long MESSAGE_EXPIRATION_MILLIS = 300000;
  /** the message queue name */
  public static final String MSG_QUEUE_NAME = "queue/mdb";
  /** the number of terms per database shard */
  public static final int NBR_TERMS_PER_DB_SHARD = 70000;
  /** the string builder initial size */
  public static final int STRING_BUILDER_SIZE = 2000;
  /** the small string builder initial size */
  public static final int STRING_BUILDER_SIZE_SMALL = 200;
  /** the string value column size */
  public static final int STRING_VALUE_SIZE = 4096;
  /** the number of milliseconds in ten seconds */
  public static final int TEN_SECONDS = 10000;
  /** the transaction timeout */
  public static final int TRANSACTION_TIMEOUT_SECONDS = 300;
  /** the name of the user.home property */
  public static final String USER_HOME = "user.home";
  /** the name of the path.separator property */
  public static final String PATH_SEPARATOR = "path.separator";
  /** the name of the file.separator property */
  public static final String FILE_SEPARATOR = "file.separator";
  /** the extension for index files */
  public static final String INDX_FILE_EXTENSION = "indx";
  /** the RDF entity loader pool size */
  public static final int RDF_ENTITY_LOADER_POOL_MAX_ACTIVE = 10;
  /** the add operation */
  public static final String ADD_OPERATION = "add";
  /** the remove operation */
  public static final String REMOVE_OPERATION = "remove";

  /** Creates a new instance of Constants but this class is never instantiated. */
  protected Constants() {
    super();
  }
}
