/* Based on Jan Daciuk's code from www.eti.pg.gda.pl/~jandac/fsa.html */

#include	<iostream>
#include	<fstream>
#include	<string.h>
#include	<stdlib.h>
#include	<new>
#include	"majka.h"

struct signature { // dictionary file signature
  char			sig[4];		// automaton identifier (magic number)
  char			ver;		// automaton type number (not used in fact)
  char			filler;		// char used as filler (not used)
  char			annot_sep;	// char that separates annotations from lex (not used)
  char			goto_length;	// length of go_to field
  char			type;
  char			version_major;
  unsigned short int	version_minor;
  unsigned short int	max_result;
  unsigned short int	max_results_count;
  unsigned int		max_results_size;
};

int fsa::read_fsa(const char * const dict_file_name) {
  const int	version = 5;
  streampos	file_ptr;
  long int	fsa_size;
  signature	sig_arc;

  // open dictionary file
  ifstream dict_file(dict_file_name, ios::in | ios::ate | ios::binary);
  if (dict_file.fail()) {
    cerr << "Cannot open dictionary file " << dict_file_name << endl;
    return 2;
  }
  fsa_size = (long int) dict_file.tellg() - sizeof(sig_arc);
  if (!dict_file.seekg(0L)) {
    cerr << "Seek on dictionary file " << dict_file_name << " failed" << endl;
    return 3;
  }

  // read and verify signature
  if (!(dict_file.read((char *)&sig_arc, sizeof(sig_arc)))) {
    cerr << "Cannot read a signature of dictionary file " << dict_file_name << endl;
    return 4;
  }
  if (strncmp(sig_arc.sig, "\\fsa", (size_t)4)) {
    cerr << "Invalid dictionary file (bad magic number): " << dict_file_name << endl;
    return 5;
  }
#define MYVERSION 1
  if (sig_arc.version_major != MYVERSION) {
    cerr << "Invalid majka dictionary version (" << sig_arc.version_major << " instead of " << MYVERSION << ") "
         << "of dictionary file " << dict_file_name << endl;
    return 6;
  }
  version_major = sig_arc.version_major;
  if (sig_arc.ver != version) {
    cerr << "Invalid fsa dictionary version (" << int(sig_arc.ver) << " instead of 5) "
         << "of dictionary file " << dict_file_name << endl;
    return 61;
  }
  max_result		= sig_arc.max_result;
  max_results_count	= sig_arc.max_results_count;
  _max_results_size	= sig_arc.max_results_size;
  max_results_size	= _max_results_size + 2 * (max_word_length + 2);
  type			= sig_arc.type;
  version_minor		= sig_arc.version_minor;
  goto_length		= sig_arc.goto_length & 0x0f;

  // allocate memory and read the automaton, + sizeof(size_t) due to bytes2int :-)
  dict = new unsigned char[fsa_size + sizeof(size_t)];
  if (!(dict_file.read((char *) dict, fsa_size))) {
    cerr << "Cannot read dictionary file " << dict_file_name << endl;
    delete [] dict;
    return 7;
  }
  return 0;
}

#define forallnodes(node, i) for (int i = 1; i; i = !(node[goto_offset] & 2), node += goto_offset + goto_length)

fsa::fsa(const char * const dict_name) {
  if ((state = read_fsa(dict_name))) return;

  start = first_node();
  start1 = start2 = NULL;

  arc_pointer next_node;
  next_node = set_next_node(start);
  forallnodes(next_node, i) {
    if ('!' == get_letter(next_node)) {
      start1 = next_node;
      break;
      }
    }
  next_node = set_next_node(start);
  forallnodes(next_node, j) {
    if ('^' == get_letter(next_node)) {
      start2 = next_node;
      break;
      }
    }

  for (int i = 0; i < 256; i++) table[i] = i;
  table[161] = 'A'; table[177] = 'a'; // •π
  table[163] = 'L'; table[179] = 'l'; // £≥
  table[165] = 'L'; table[181] = 'l'; // ºæ
  table[166] = 'S'; table[182] = 's'; // åú
  table[169] = 'S'; table[185] = 's'; // äö
  table[170] = 'S'; table[186] = 's'; // ™∫
  table[171] = 'T'; table[187] = 't'; // çù
  table[172] = 'Z'; table[188] = 'z'; // èü
  table[174] = 'Z'; table[190] = 'z'; // éû
  table[175] = 'Z'; table[191] = 'z'; // Øø
  table[192] = 'R'; table[224] = 'r'; // ¿‡
  table[193] = 'A'; table[225] = 'a'; // ¡·
  table[194] = 'A'; table[226] = 'a'; // ¬‚
  table[195] = 'A'; table[227] = 'a'; // √„
  table[196] = 'A'; table[228] = 'a'; // ƒ‰
  table[197] = 'L'; table[229] = 'l'; // ≈Â
  table[198] = 'C'; table[230] = 'c'; // ∆Ê
  table[199] = 'C'; table[231] = 'c'; // «Á
  table[200] = 'C'; table[232] = 'c'; // »Ë
  table[201] = 'E'; table[233] = 'e'; // …È
  table[202] = 'E'; table[234] = 'e'; //  Í
  table[203] = 'E'; table[235] = 'e'; // ÀÎ
  table[204] = 'E'; table[236] = 'e'; // ÃÏ
  table[205] = 'I'; table[237] = 'i'; // ÕÌ
  table[206] = 'I'; table[238] = 'i'; // ŒÓ
  table[207] = 'D'; table[239] = 'd'; // œÔ
  table[208] = 'D'; table[240] = 'd'; // –
  table[209] = 'N'; table[241] = 'n'; // —Ò
  table[210] = 'N'; table[242] = 'n'; // “Ú
  table[211] = 'O'; table[243] = 'o'; // ”Û
  table[212] = 'O'; table[244] = 'o'; // ‘Ù
  table[213] = 'O'; table[245] = 'o'; // ’ı
  table[214] = 'O'; table[246] = 'o'; // ÷ˆ
  table[216] = 'R'; table[248] = 'r'; // ÿ¯
  table[217] = 'U'; table[249] = 'u'; // Ÿ˘
  table[218] = 'U'; table[250] = 'u'; // ⁄˙
  table[219] = 'U'; table[251] = 'u'; // €˚
  table[220] = 'U'; table[252] = 'u'; // ‹¸
  table[221] = 'Y'; table[253] = 'y'; // ›˝
  table[222] = 'T'; table[254] = 't'; // ﬁ˛

  for (int i = 0; i < 256; i++) table[256 + i] = tablelc[i] = i >= 'A' && i <= 'Z' ? i + 'a' - 'A' : i;
  for (int i = 161; i < 176; i++) table[256 + i] = tablelc[i] = i + 16;
  for (int i = 192; i < 223; i++) table[256 + i] = tablelc[i] = i + 32;

  for (int i = 0; i < 256; i++) table[512 + i] = table[table[256 + i]];

  for (int i = 0; i < 256; i++) table1[i] = table2[i] = i; // ISO-8859-2 <-> Windows-1250
  table1[169] = 138; table2[138] = 169; // ä
  table1[166] = 140; table2[140] = 166; // å
  table1[171] = 141; table2[141] = 171; // ç
  table1[174] = 142; table2[142] = 174; // é
  table1[172] = 143; table2[143] = 172; // è
  table1[185] = 154; table2[154] = 185; // ö
  table1[182] = 156; table2[156] = 182; // ú
  table1[187] = 157; table2[157] = 187; // ù
  table1[190] = 158; table2[158] = 190; // û
  table1[188] = 159; table2[159] = 188; // ü
  table1[161] = 165; table2[165] = 161; // •
  table1[177] = 185; table2[185] = 177; // π
  table1[165] = 188; table2[188] = 165; // º
  table1[181] = 190; table2[190] = 181; // æ
}

// Rather ugly temporary solution...
#define candidate res.candidate
#define result res.result
#define results_count res.results_count
#define input_len res.input_len
int fsa::find(const char * const sought, char * const results_buf, const char flags) {
  unsigned char * copy = (unsigned char *) results_buf + _max_results_size;
  thread_specific res;

  candidate = (unsigned char *) results_buf + _max_results_size + max_word_length + 2;
  result = (unsigned char *) results_buf;
  results_count = 0;

  unsigned char * j = copy;
  const unsigned char * tmp = (const unsigned char *) sought + max_word_length;
  char uppercase = 0;
  for (const unsigned char * i = (const unsigned char *) sought; *i && i < tmp; i++, j++) {
    *j = table2[*i];
    if (! (flags & (IGNORE_CASE | DISALLOW_LOWERCASE)) && j != copy && tablelc[*j] != *j) uppercase = 1;
  }
  input_len = j - copy;
  *j = ':';
  *(j + 1) = '\0';

  if (flags & (ADD_DIACRITICS | IGNORE_CASE)) {
    const unsigned char * accent_table = table + 256 * (flags - 1);
    if (flags & IGNORE_CASE) for (unsigned char * i = copy; *i; i++) *i = tablelc[*i];
    accent_word(copy, 0, start, NULL, accent_table, res);
    if (uppercase) {
      for (unsigned char * i = (copy + 1); *i; i++) *i = tablelc[*i];
      accent_word(copy, 0, start, NULL, accent_table, res);
    }
    if (tablelc[*copy] != *copy) {
      *copy = tablelc[*copy];
      accent_word(copy, 0, start, NULL, accent_table, res);
    }
    if ((! results_count) && start1 && start2) accent_word(copy, 0, start1, start2, accent_table, res);
  }
  else {
    find_word(copy, 0, start, res);
    if (uppercase) {
      for (unsigned char * i = (copy + 1); *i; i++) *i = tablelc[*i];
      find_word(copy, 0, start, res);
    }
    if (tablelc[*copy] != *copy && ! (flags & DISALLOW_LOWERCASE)) {
      *copy = tablelc[*copy];
      find_word(copy, 0, start, res);
    }
    if (! results_count && start1 && start2) {
      arc_pointer new_node, next_node = set_next_node(start1);
      int level = 0;
      bool found = false;
      unsigned char * word = copy;

      do {
        found = false;
        forallnodes(next_node, i)
          if (*word == get_letter(next_node)) {
            candidate[level++] = get_letter(next_node);
            if (*++word == '\0') return results_count;
            found = true;
            new_node = next_node = set_next_node(next_node);
            break;
          }
        if (found) forallnodes(new_node, j)
          if (':' == get_letter(new_node)) {
            find_word(word, level, start2, res);
            break;
          }
      } while (found);
    }
  }
  return results_count;
}

void fsa::accent_word(const unsigned char * const word, const int level, arc_pointer next_node, const arc_pointer start_node2, const unsigned char * accent_table, thread_specific &res) {
  next_node = set_next_node(next_node);
  unsigned char	char_no;
  forallnodes(next_node, i) {
    char_no = get_letter(next_node);
    if (*word == char_no || *word == accent_table[char_no]) {
      candidate[level] = get_letter(next_node);
      if (word[1] == '\0' && ! start_node2) compl_rest(level + 1, next_node, res);
      else accent_word(word + 1, level + 1, next_node, start_node2, accent_table, res);
    }
    else if (get_letter(next_node) == ':' && start_node2) accent_word(word, level, start_node2, NULL, accent_table, res);
  }
}

void fsa::find_word(const unsigned char * word, int level, arc_pointer next_node, thread_specific &res) {
  next_node = set_next_node(next_node);
  bool found;
  do {
    found = false;
    forallnodes(next_node, i) {
      if (*word == get_letter(next_node)) {
        candidate[level++] = get_letter(next_node);
	if (word[1] == '\0') compl_rest(level, next_node, res); else found = ++word;
	break;
      }
    }
    if (found) next_node = set_next_node(next_node);
  } while (found);
}

void fsa::compl_rest(const int depth, arc_pointer next_node, thread_specific &res) {
  next_node = set_next_node(next_node);
  if (next_node == dict) return;
  forallnodes(next_node, i) {
    candidate[depth] = get_letter(next_node);
    if (is_final(next_node)) {
      candidate[depth + 1] = '\0';
      process_result(res);
      results_count++;
    }
    compl_rest(depth + 1, next_node, res);
  }
}

void fsa::process_result(thread_specific &res) { switch (type) { // not indented

case 1:   // w-lt
case 4: { // l-wt
  my_strncpy(result, candidate, input_len - (candidate[input_len + 1] - 'A'));
  my_strxcpy(result, candidate + input_len + 2);
} break;

case 3: { // lt-w
  // We want to allow l-tw queries also (we would have to check a presence of [the "first"] ':' in the query otherwise)
  const unsigned char * const first = (unsigned char *) strchr((char *) candidate, ':');
  const unsigned char * second = candidate + input_len;
  if (first == second) {
    second = (unsigned char *) strchr((char *) first + 1, ':');
    my_strncpy(result, first + 1, second - first);
    }
  my_strncpy(result, candidate, first - candidate - (second[1] - 'A'));
  my_strcpy(result, second + 2);
} break;

case 2:	  // w
case 5:   // l-w
case 6:   // w-l
case 7: { // w-w
  my_strncpy(result, candidate, input_len - (candidate[input_len + 1] - 'A'));
  my_strcpy(result, candidate + input_len + 2);
} break;

case 1 + 128: { // w-lt
  int prefix_len = candidate[input_len + 1] - 'A';
  my_strncpy(result, candidate + prefix_len, input_len - prefix_len - (candidate[input_len + 2] - 'A'));
  my_strxcpy(result, candidate + input_len + 3);
} break;

case 2 + 128: { // w
  my_strncpy(result, candidate, input_len);
  *result++ = '\0';
} break;

case 3 + 128: { // lt-w
  const unsigned char * const first = (unsigned char *) strchr((char *) candidate, ':');
  const unsigned char * second = candidate + input_len;
  if (first == second) {
    second = (unsigned char *) strchr((char *) first + 1, ':');
    my_strncpy(result, first + 1, second - first);
    }
  int prefix_len = second[1] - 'A';
  my_strncpy(result, second + 2, prefix_len);
  my_strncpy(result, candidate, first - candidate - (second[prefix_len + 2] - 'A'));
  my_strcpy(result, second + prefix_len + 3);
} break;

case 4 + 128: { // l-wt
  int prefix_len = candidate[input_len + 1] - 'A';
  my_strncpy(result, candidate + input_len + 2, prefix_len);
  my_strncpy(result, candidate, input_len - (candidate[input_len + prefix_len + 2] - 'A'));
  my_strxcpy(result, candidate + input_len + prefix_len + 3);
} break;

case 5 + 128: { // l-w
  int prefix_len = candidate[input_len + 1] - 'A';
  my_strncpy(result, candidate + input_len + 2, prefix_len);
  my_strncpy(result, candidate, input_len - (candidate[input_len + prefix_len + 2] - 'A'));
  my_strcpy(result, candidate + input_len + prefix_len + 3);
} break;

case 6 + 128: { // w-l
  int prefix_len = candidate[input_len + 1] - 'A';
  my_strncpy(result, candidate + prefix_len, input_len - prefix_len - (candidate[input_len + 2] - 'A'));
  my_strcpy(result, candidate + input_len + 3);
} break;

case 7 + 128: { // w-w
  int prefix_add_len = candidate[input_len + 1] - 'A';
  my_strncpy(result, candidate + input_len + 2, prefix_add_len);
  int prefix_remove_len = candidate[input_len + 2 + prefix_add_len] - 'A';
  my_strncpy(result, candidate + prefix_remove_len, input_len - prefix_remove_len - (candidate[input_len + 3 + prefix_add_len] - 'A'));
  my_strcpy(result, candidate + input_len + prefix_add_len + 4);
} break;

default:
  cerr << "Invalid dictionary file (cannot interpret file of type " << (short int) type << ")" << endl;
  // Yes, exiting from a library is rather ugly, but it should not happen and this is the simplest solution
  exit(EXIT_FAILURE); // And of course it does not free the memory, but again, this should not happen :-)
}}
