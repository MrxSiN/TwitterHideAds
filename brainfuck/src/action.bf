# action bf: promoted post action names found by the fallback graph walk
# Normative specification: docs/policy/action md ABI: docs/BRAINFUCK_ARCHITECTURE md
# Serves OP_ACTION_NAMES Two machines share one trie of the three action words:
# TS matches the text after the last hash exactly and SS follows every suffix
#
# Tape layout
# %cell MAJ 0
# %cell OP 1
# %cell ID0 2
# %cell ID1 3
# %cell J 4
# %cell UNH 5
# %cell N 6
# %cell L0 7
# %cell L1 8
# %cell ANY 9
# %cell ISENUM 10
# %cell ISACT 11
# %cell TS 12
# %cell NT 13
# %cell SS 14
# %cell NSS 15
# %cell K 16
# %cell CH 17
# %cell R 18
# %cell t0 24   scratch; zero whenever free
# %cell t1 25   scratch; zero whenever free
# %cell t2 26   scratch; zero whenever free
# %cell t3 27   scratch; zero whenever free
# %cell t4 28   scratch; zero whenever free
# %cell t5 29   scratch; zero whenever free
# %cell t6 30   scratch; zero whenever free
# %cell t7 31   scratch; zero whenever free
# %cell t8 32   scratch; zero whenever free
# %cell t9 33   scratch; zero whenever free
# %cell t10 34   scratch; zero whenever free
# %cell t11 35   scratch; zero whenever free
# %cell t12 36   scratch; zero whenever free
# %cell t13 37   scratch; zero whenever free
# %cell t14 38   scratch; zero whenever free
# %cell t15 39   scratch; zero whenever free
# %cell t16 40   scratch; zero whenever free
# %cell t17 41   scratch; zero whenever free
# %cell t18 42   scratch; zero whenever free
# %cell t19 43   scratch; zero whenever free
# %cell t20 44   scratch; zero whenever free
# %cell t21 45   scratch; zero whenever free
# %cell t22 46   scratch; zero whenever free
# %cell t23 47   scratch; zero whenever free
# %cell t24 48   scratch; zero whenever free
# %cell t25 49   scratch; zero whenever free
# %cell t26 50   scratch; zero whenever free
# %cell t27 51   scratch; zero whenever free
# %cell t28 52   scratch; zero whenever free
# %cell t29 53   scratch; zero whenever free

# request header: major minor opcode flags length request id
, @MAJ                                    # read MAJ
>>>> , @J                                 # minor is not checked
[-] @J                                    # clear J
<<< , @OP                                 # read OP
>>> , @J                                  # flags ignored; payload layout is fixed per opcode
[-] @J                                    # clear J
, @J                                      # length low ignored; payload layout is fixed per opcode
[-] @J                                    # clear J
, @J                                      # length high ignored; payload layout is fixed per opcode
[-] @J                                    # clear J
<< , @ID0                                 # request id low
> , @ID1                                  # request id high

# ABI major version
<<< [- >>>>>>>>>> >>>>>>>>>> >>>> ~24+ >+ <<<<<<<<<< <<<<<<<<<< <<<<< ~25] @MAJ  # copy MAJ
>>>>>>>>>> >>>>>>>>>> >>>>> ~25 [- <<<<<<<<<< <<<<<<<<<< <<<<< ~25+ >>>>>>>>>> >>>>>>>>>> >>>>> ~25] @t1  # move t1 into MAJ
< - @t0                                   # subtract 1
>> + @t2                                  # assume equal
<< [ @t0                                  # if different
    >> [-] @t2                            # not equal
    # response header
    > + @t3                               # major
    . @t3                                 # write t3
    [-] @t3                               # clear t3
    . @t3                                 # write t3
    [-] @t3                               # clear t3
    <<<<<<<<<< <<<<<<<<<< <<<<<< ~26 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @OP  # copy OP to t3
    >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t4  # move t4 into OP
    < ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++ ~128 @t3  # opcode with the response bit
    . @t3                                 # write t3
    [-] @t3                               # clear t3
    + @t3                                 # status
    . @t3                                 # write t3
    [-] @t3                               # clear t3
    . @t3                                 # write t3
    [-] @t3                               # clear t3
    . @t3                                 # write t3
    [-] @t3                               # clear t3
    <<<<<<<<<< <<<<<<<<<< <<<<< ~25 . @ID0  # echo request id
    > . @ID1                              # write ID1
    >>>>>>>>>> >>>>>>>>>> > ~21 [-] @t0   # clear t0
] @t0
>> [ @t2                                  # if equal
    <<<<<<<<<< <<<<<<<<<< < ~21 [-] @UNH  # clear UNH
    + @UNH                                # UNH plus 1
    # opcode 32
    <<<< [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @OP  # copy OP
    >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t4  # move t4 into OP
    < ---------- ---------- ---------- -- ~32 @t3  # subtract 32
    >> + @t5                              # assume equal
    << [ @t3                              # if different
        >> [-] @t5                        # not equal
        << [-] @t3                        # clear t3
    ] @t3
    >> [ @t5                              # if equal
        <<<<<<<<<< <<<<<<<<<< <<<< ~24 [-] @UNH  # clear UNH
        # OP ACTION NAMES
        > , @N                            # number of scalar values
        [- >+ >>>>>>>>>> >>>>>>>>>> >>> ~23+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @N  # copy N to L0
        >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t6  # move t6 into N
        <<<<<<<<<< <<<<<<<<<< <<< ~23 + @L0  # L0 plus 1
        # carry
        [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @L0  # copy L0
        >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t7  # move t7 into L0
        > + @t8                           # assume equal
        << [ @t6                          # if different
            >> [-] @t8                    # not equal
            << [-] @t6                    # clear t6
        ] @t6
        >> [ @t8                          # if equal
            <<<<<<<<<< <<<<<<<<<< <<<< ~24 + @L1  # L1 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>> ~24 [-] @t8  # clear t8
        ] @t8
        # response header
        << + @t6                          # major
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~29 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~29+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30] @OP  # copy OP to t6
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30] @t7  # move t7 into OP
        < ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++ ~128 @t6  # opcode with the response bit
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<< ~23 . @L0  # payload length low
        > . @L1                           # payload length high
        <<<<<< . @ID0                     # echo request id
        > . @ID1                          # write ID1
        >>>> [-] @L0                      # clear L0
        > [-] @L1                         # clear L1
        << [ @N                           # each value
            - @N                          # N minus 1
            >>>> , @ISENUM                # value is an enum constant name
            [ @ISENUM                     # normalize ISENUM
                >>>>>>>>>> >>>>>>>>>> ~20 + @t6  # t6 plus 1
                <<<<<<<<<< <<<<<<<<<< ~20 [-] @ISENUM  # clear ISENUM
            ] @ISENUM
            >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t6  # move t6 into ISENUM
            <<<<<<<<<< <<<<<<<<< ~19 , @ISACT  # value belongs to the action enum class
            [ @ISACT                      # normalize ISACT
                >>>>>>>>>> >>>>>>>>> ~19 + @t6  # t6 plus 1
                <<<<<<<<<< <<<<<<<<< ~19 [-] @ISACT  # clear ISACT
            ] @ISACT
            >>>>>>>>>> >>>>>>>>> ~19 [- <<<<<<<<<< <<<<<<<<< ~19+ >>>>>>>>>> >>>>>>>>> ~19] @t6  # move t6 into ISACT
            <<<<<<<<<< <<<< ~14 , @K      # first chunk length
            [ @K                          # each chunk
                [ @K                      # each character of the chunk
                    - @K                  # K minus 1
                    > , @CH               # read CH
                    # one character
                    # case CH equals 1
                    [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < - @t6               # subtract 1
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # a hash restarts the exact match unless the value is an enum name
                        <<<<<<<<<< <<<<<<<<<< << ~22 [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @ISENUM  # copy ISENUM to t9
                        >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t10  # move t10 into ISENUM
                        < [ @t9           # enum names keep the hash
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NT  # NT becomes 99
                            ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t9  # clear t9
                        ] @t9
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        > [-] @SS         # clear SS
                        >>>>>>>>>> >>>>>>>> ~18 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 0
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    > + @t8               # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        <<<<<<<<<< <<<<<<<<<< ~20 [-] @TS  # TS becomes 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @TS  # TS plus 99
                        >> [-] @SS        # clear SS
                        >>>>>>>>>> >>>>>>>> ~18 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 2
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < -- @t6              # subtract 2
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter P
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 0
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        > + @t11          # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 1
                            + @NT         # NT plus 1
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 1
                        + @NSS            # NSS plus 1
                        # suffix state 0
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        > + @t11          # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 1
                            + @NSS        # NSS plus 1
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 3
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < --- @t6             # subtract 3
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter r
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 1
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < - @t9           # subtract 1
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 2
                            ++ @NT        # NT plus 2
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 28
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ---------- -------- ~28 @t9  # subtract 28
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 29
                            ++++++++++ ++++++++++ +++++++++ ~29 @NT  # NT plus 29
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 1
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < - @t9           # subtract 1
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 2
                            ++ @NSS       # NSS plus 2
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 28
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ---------- -------- ~28 @t9  # subtract 28
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 29
                            ++++++++++ ++++++++++ +++++++++ ~29 @NSS  # NSS plus 29
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 4
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < ---- @t6            # subtract 4
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter o
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 2
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < -- @t9          # subtract 2
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 3
                            +++ @NT       # NT plus 3
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 4
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---- @t9        # subtract 4
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 5
                            +++++ @NT     # NT plus 5
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 23
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ---------- --- ~23 @t9  # subtract 23
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 24
                            ++++++++++ ++++++++++ ++++ ~24 @NT  # NT plus 24
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 27
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ---------- ------- ~27 @t9  # subtract 27
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 28
                            ++++++++++ ++++++++++ ++++++++ ~28 @NT  # NT plus 28
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 2
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < -- @t9          # subtract 2
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 3
                            +++ @NSS      # NSS plus 3
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 4
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---- @t9        # subtract 4
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 5
                            +++++ @NSS    # NSS plus 5
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 23
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ---------- --- ~23 @t9  # subtract 23
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 24
                            ++++++++++ ++++++++++ ++++ ~24 @NSS  # NSS plus 24
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 27
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ---------- ------- ~27 @t9  # subtract 27
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 28
                            ++++++++++ ++++++++++ ++++++++ ~28 @NSS  # NSS plus 28
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 5
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < ----- @t6           # subtract 5
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter m
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 3
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < --- @t9         # subtract 3
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 4
                            ++++ @NT      # NT plus 4
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- - ~11 @t9  # subtract 11
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 12
                            ++++++++++ ++ ~12 @NT  # NT plus 12
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 3
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < --- @t9         # subtract 3
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 4
                            ++++ @NSS     # NSS plus 4
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- - ~11 @t9  # subtract 11
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 12
                            ++++++++++ ++ ~12 @NSS  # NSS plus 12
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 6
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < ------ @t6          # subtract 6
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter t
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 5
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ----- @t9       # subtract 5
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 6
                            ++++++ @NT    # NT plus 6
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 29
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ---------- --------- ~29 @t9  # subtract 29
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 30
                            ++++++++++ ++++++++++ ++++++++++ ~30 @NT  # NT plus 30
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 5
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ----- @t9       # subtract 5
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 6
                            ++++++ @NSS   # NSS plus 6
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 29
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ---------- --------- ~29 @t9  # subtract 29
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 30
                            ++++++++++ ++++++++++ ++++++++++ ~30 @NSS  # NSS plus 30
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 7
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < ------- @t6         # subtract 7
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter e
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 6
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ------ @t9      # subtract 6
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 7
                            +++++++ @NT   # NT plus 7
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 25
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ---------- ----- ~25 @t9  # subtract 25
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 26
                            ++++++++++ ++++++++++ ++++++ ~26 @NT  # NT plus 26
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 6
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ------ @t9      # subtract 6
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 7
                            +++++++ @NSS  # NSS plus 7
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 25
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ---------- ----- ~25 @t9  # subtract 25
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 26
                            ++++++++++ ++++++++++ ++++++ ~26 @NSS  # NSS plus 26
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 8
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < -------- ~8 @t6     # subtract 8
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter d
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 7
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ------- @t9     # subtract 7
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 8
                            ++++++++ ~8 @NT  # NT plus 8
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 16
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ------ ~16 @t9  # subtract 16
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 17
                            ++++++++++ +++++++ ~17 @NT  # NT plus 17
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 18
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- -------- ~18 @t9  # subtract 18
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 19
                            ++++++++++ +++++++++ ~19 @NT  # NT plus 19
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 31
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ---------- ---------- - ~31 @t9  # subtract 31
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 32
                            ++++++++++ ++++++++++ ++++++++++ ++ ~32 @NT  # NT plus 32
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 7
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ------- @t9     # subtract 7
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 8
                            ++++++++ ~8 @NSS  # NSS plus 8
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 16
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ------ ~16 @t9  # subtract 16
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 17
                            ++++++++++ +++++++ ~17 @NSS  # NSS plus 17
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 18
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- -------- ~18 @t9  # subtract 18
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 19
                            ++++++++++ +++++++++ ~19 @NSS  # NSS plus 19
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 31
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ---------- ---------- - ~31 @t9  # subtract 31
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 32
                            ++++++++++ ++++++++++ ++++++++++ ++ ~32 @NSS  # NSS plus 32
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 9
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < --------- ~9 @t6    # subtract 9
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter D
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 8
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < -------- ~8 @t9  # subtract 8
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 9
                            +++++++++ ~9 @NT  # NT plus 9
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 8
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < -------- ~8 @t9  # subtract 8
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 9
                            +++++++++ ~9 @NSS  # NSS plus 9
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 10
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < ---------- ~10 @t6  # subtract 10
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter i
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 9
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < --------- ~9 @t9  # subtract 9
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 10
                            ++++++++++ ~10 @NT  # NT plus 10
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 12
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- -- ~12 @t9  # subtract 12
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 13
                            ++++++++++ +++ ~13 @NT  # NT plus 13
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 9
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < --------- ~9 @t9  # subtract 9
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 10
                            ++++++++++ ~10 @NSS  # NSS plus 10
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 12
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- -- ~12 @t9  # subtract 12
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 13
                            ++++++++++ +++ ~13 @NSS  # NSS plus 13
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 11
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < ---------- - ~11 @t6  # subtract 11
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter s
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 10
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ~10 @t9  # subtract 10
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 11
                            ++++++++++ + ~11 @NT  # NT plus 11
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 13
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- --- ~13 @t9  # subtract 13
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 14
                            ++++++++++ ++++ ~14 @NT  # NT plus 14
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 14
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ---- ~14 @t9  # subtract 14
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 15
                            ++++++++++ +++++ ~15 @NT  # NT plus 15
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 19
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- --------- ~19 @t9  # subtract 19
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 20
                            ++++++++++ ++++++++++ ~20 @NT  # NT plus 20
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 10
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ~10 @t9  # subtract 10
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 11
                            ++++++++++ + ~11 @NSS  # NSS plus 11
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 13
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- --- ~13 @t9  # subtract 13
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 14
                            ++++++++++ ++++ ~14 @NSS  # NSS plus 14
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 14
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ---- ~14 @t9  # subtract 14
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 15
                            ++++++++++ +++++ ~15 @NSS  # NSS plus 15
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 19
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- --------- ~19 @t9  # subtract 19
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 20
                            ++++++++++ ++++++++++ ~20 @NSS  # NSS plus 20
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 12
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < ---------- -- ~12 @t6  # subtract 12
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter A
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 15
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ----- ~15 @t9  # subtract 15
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 16
                            ++++++++++ ++++++ ~16 @NT  # NT plus 16
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 8
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < -------- ~8 @t9  # subtract 8
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 18
                            ++++++++++ ++++++++ ~18 @NT  # NT plus 18
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        # exact state 30
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ---------- ---------- ~30 @t9  # subtract 30
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 31
                            ++++++++++ ++++++++++ ++++++++++ + ~31 @NT  # NT plus 31
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 15
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ----- ~15 @t9  # subtract 15
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 16
                            ++++++++++ ++++++ ~16 @NSS  # NSS plus 16
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 8
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < -------- ~8 @t9  # subtract 8
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 18
                            ++++++++++ ++++++++ ~18 @NSS  # NSS plus 18
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        # suffix state 30
                        <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ---------- ---------- ~30 @t9  # subtract 30
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 31
                            ++++++++++ ++++++++++ ++++++++++ + ~31 @NSS  # NSS plus 31
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 13
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < ---------- --- ~13 @t6  # subtract 13
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter I
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 20
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ---------- ~20 @t9  # subtract 20
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 21
                            ++++++++++ ++++++++++ + ~21 @NT  # NT plus 21
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 20
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ---------- ~20 @t9  # subtract 20
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 21
                            ++++++++++ ++++++++++ + ~21 @NSS  # NSS plus 21
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 14
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < ---------- ---- ~14 @t6  # subtract 14
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter n
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 21
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ---------- - ~21 @t9  # subtract 21
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 22
                            ++++++++++ ++++++++++ ++ ~22 @NT  # NT plus 22
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 21
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ---------- - ~21 @t9  # subtract 21
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 22
                            ++++++++++ ++++++++++ ++ ~22 @NSS  # NSS plus 22
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 15
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < ---------- ----- ~15 @t6  # subtract 15
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter f
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 22
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ---------- -- ~22 @t9  # subtract 22
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 23
                            ++++++++++ ++++++++++ +++ ~23 @NT  # NT plus 23
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 22
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ---------- -- ~22 @t9  # subtract 22
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 23
                            ++++++++++ ++++++++++ +++ ~23 @NSS  # NSS plus 23
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 16
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < ---------- ------ ~16 @t6  # subtract 16
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter R
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 8
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < -------- ~8 @t9  # subtract 8
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 25
                            ++++++++++ ++++++++++ +++++ ~25 @NT  # NT plus 25
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 8
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < -------- ~8 @t9  # subtract 8
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 25
                            ++++++++++ ++++++++++ +++++ ~25 @NSS  # NSS plus 25
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    # case CH equals 17
                    <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @CH  # copy CH
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t7  # move t7 into CH
                    < ---------- ------- ~17 @t6  # subtract 17
                    >> + @t8              # assume equal
                    << [ @t6              # if different
                        >> [-] @t8        # not equal
                        << [-] @t6        # clear t6
                    ] @t6
                    >> [ @t8              # if equal
                        # letter p
                        <<<<<<<<<< <<<<<<<<< ~19 [-] @NT  # NT is 99
                        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++++++ ~99 @NT  # NT plus 99
                        # exact state 26
                        < [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @TS  # copy TS
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t10  # move t10 into TS
                        < ---------- ---------- ------ ~26 @t9  # subtract 26
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @NT  # NT becomes 27
                            ++++++++++ ++++++++++ +++++++ ~27 @NT  # NT plus 27
                            >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @TS  # clear TS
                        > [- <+ >] @NT    # TS takes NT
                        >> [-] @NSS       # NSS is 0
                        # suffix state 26
                        < [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @SS  # copy SS
                        >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t10  # move t10 into SS
                        < ---------- ---------- ------ ~26 @t9  # subtract 26
                        >> + @t11         # assume equal
                        << [ @t9          # if different
                            >> [-] @t11   # not equal
                            << [-] @t9    # clear t9
                        ] @t9
                        >> [ @t11         # if equal
                            <<<<<<<<<< <<<<<<<<<< ~20 [-] @NSS  # NSS becomes 27
                            ++++++++++ ++++++++++ +++++++ ~27 @NSS  # NSS plus 27
                            >>>>>>>>>> >>>>>>>>>> ~20 [-] @t11  # clear t11
                        ] @t11
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @SS  # clear SS
                        > [- <+ >] @NSS   # SS takes NSS
                        >>>>>>>>>> >>>>>>> ~17 [-] @t8  # clear t8
                    ] @t8
                    <<<<<<<<<< <<<<< ~15 [-] @CH  # clear CH
                < ] @K
                , @K                      # next chunk length
            ] @K
            # exact match of the text after the last hash
            # word end 17
            <<<< [- >>>>>>>>>> >>>>>>>> ~18+ >+ <<<<<<<<<< <<<<<<<<< ~19] @TS  # copy TS
            >>>>>>>>>> >>>>>>>>> ~19 [- <<<<<<<<<< <<<<<<<<< ~19+ >>>>>>>>>> >>>>>>>>> ~19] @t7  # move t7 into TS
            < ---------- ------- ~17 @t6  # subtract 17
            >> + @t8                      # assume equal
            << [ @t6                      # if different
                >> [-] @t8                # not equal
                << [-] @t6                # clear t6
            ] @t6
            >> [ @t8                      # if equal
                <<<<<<<<<< <<<< ~14 [-] @R  # R becomes 1
                + @R                      # R plus 1
                >>>>>>>>>> >>>> ~14 [-] @t8  # clear t8
            ] @t8
            # word end 24
            <<<<<<<<<< <<<<<<<<<< ~20 [- >>>>>>>>>> >>>>>>>> ~18+ >+ <<<<<<<<<< <<<<<<<<< ~19] @TS  # copy TS
            >>>>>>>>>> >>>>>>>>> ~19 [- <<<<<<<<<< <<<<<<<<< ~19+ >>>>>>>>>> >>>>>>>>> ~19] @t7  # move t7 into TS
            < ---------- ---------- ---- ~24 @t6  # subtract 24
            >> + @t8                      # assume equal
            << [ @t6                      # if different
                >> [-] @t8                # not equal
                << [-] @t6                # clear t6
            ] @t6
            >> [ @t8                      # if equal
                <<<<<<<<<< <<<< ~14 [-] @R  # R becomes 2
                ++ @R                     # R plus 2
                >>>>>>>>>> >>>> ~14 [-] @t8  # clear t8
            ] @t8
            # word end 32
            <<<<<<<<<< <<<<<<<<<< ~20 [- >>>>>>>>>> >>>>>>>> ~18+ >+ <<<<<<<<<< <<<<<<<<< ~19] @TS  # copy TS
            >>>>>>>>>> >>>>>>>>> ~19 [- <<<<<<<<<< <<<<<<<<< ~19+ >>>>>>>>>> >>>>>>>>> ~19] @t7  # move t7 into TS
            < ---------- ---------- ---------- -- ~32 @t6  # subtract 32
            >> + @t8                      # assume equal
            << [ @t6                      # if different
                >> [-] @t8                # not equal
                << [-] @t6                # clear t6
            ] @t6
            >> [ @t8                      # if equal
                <<<<<<<<<< <<<< ~14 [-] @R  # R becomes 3
                +++ @R                    # R plus 3
                >>>>>>>>>> >>>> ~14 [-] @t8  # clear t8
            ] @t8
            <<<<<<<<<< <<<<<<<<<< < ~21 [- >>>>>>>>>> >>>>>>>>> ~19+ >+ <<<<<<<<<< <<<<<<<<<< ~20] @ISACT  # copy ISACT to t6
            >>>>>>>>>> >>>>>>>>>> ~20 [- <<<<<<<<<< <<<<<<<<<< ~20+ >>>>>>>>>> >>>>>>>>>> ~20] @t7  # move t7 into ISACT
            < [ @t6                       # action enum values also match by suffix
                # no exact match
                <<<<<<<<<< << ~12 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @R  # copy R
                >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t8  # move t8 into R
                > + @t9                   # assume equal
                << [ @t7                  # if different
                    >> [-] @t9            # not equal
                    << [-] @t7            # clear t7
                ] @t7
                >> [ @t9                  # if equal
                    # suffix word end 17
                    <<<<<<<<<< <<<<<<<<< ~19 [- >>>>>>>>>> >>>>>>>>>> ~20+ >+ <<<<<<<<<< <<<<<<<<<< < ~21] @SS  # copy SS
                    >>>>>>>>>> >>>>>>>>>> > ~21 [- <<<<<<<<<< <<<<<<<<<< < ~21+ >>>>>>>>>> >>>>>>>>>> > ~21] @t11  # move t11 into SS
                    < ---------- ------- ~17 @t10  # subtract 17
                    >> + @t12             # assume equal
                    << [ @t10             # if different
                        >> [-] @t12       # not equal
                        << [-] @t10       # clear t10
                    ] @t10
                    >> [ @t12             # if equal
                        <<<<<<<<<< <<<<<<<< ~18 [-] @R  # R becomes 1
                        + @R              # R plus 1
                        >>>>>>>>>> >>>>>>>> ~18 [-] @t12  # clear t12
                    ] @t12
                    # suffix word end 24
                    <<<<<<<<<< <<<<<<<<<< << ~22 [- >>>>>>>>>> >>>>>>>>>> ~20+ >+ <<<<<<<<<< <<<<<<<<<< < ~21] @SS  # copy SS
                    >>>>>>>>>> >>>>>>>>>> > ~21 [- <<<<<<<<<< <<<<<<<<<< < ~21+ >>>>>>>>>> >>>>>>>>>> > ~21] @t11  # move t11 into SS
                    < ---------- ---------- ---- ~24 @t10  # subtract 24
                    >> + @t12             # assume equal
                    << [ @t10             # if different
                        >> [-] @t12       # not equal
                        << [-] @t10       # clear t10
                    ] @t10
                    >> [ @t12             # if equal
                        <<<<<<<<<< <<<<<<<< ~18 [-] @R  # R becomes 2
                        ++ @R             # R plus 2
                        >>>>>>>>>> >>>>>>>> ~18 [-] @t12  # clear t12
                    ] @t12
                    # suffix word end 32
                    <<<<<<<<<< <<<<<<<<<< << ~22 [- >>>>>>>>>> >>>>>>>>>> ~20+ >+ <<<<<<<<<< <<<<<<<<<< < ~21] @SS  # copy SS
                    >>>>>>>>>> >>>>>>>>>> > ~21 [- <<<<<<<<<< <<<<<<<<<< < ~21+ >>>>>>>>>> >>>>>>>>>> > ~21] @t11  # move t11 into SS
                    < ---------- ---------- ---------- -- ~32 @t10  # subtract 32
                    >> + @t12             # assume equal
                    << [ @t10             # if different
                        >> [-] @t12       # not equal
                        << [-] @t10       # clear t10
                    ] @t10
                    >> [ @t12             # if equal
                        <<<<<<<<<< <<<<<<<< ~18 [-] @R  # R becomes 3
                        +++ @R            # R plus 3
                        >>>>>>>>>> >>>>>>>> ~18 [-] @t12  # clear t12
                    ] @t12
                    <<< [-] @t9           # clear t9
                ] @t9
                <<< [-] @t6               # clear t6
            ] @t6
            <<<<<<<<<< << ~12 . @R        # match
            [- >>>>>>>>>> >> ~12+ >+ <<<<<<<<<< <<< ~13] @R  # copy R to t6
            >>>>>>>>>> >>> ~13 [- <<<<<<<<<< <<< ~13+ >>>>>>>>>> >>> ~13] @t7  # move t7 into R
            < [ @t6                       # if t6 then
                <<<<<<<<<< <<<<<<<<<< < ~21 [-] @ANY  # clear ANY
                + @ANY                    # ANY plus 1
                >>>>>>>>>> >>>>>>>>>> > ~21 [-] @t6  # clear t6
            ] @t6
            <<<<<<<<<< << ~12 [-] @R      # clear R
            <<<<<< [-] @TS                # clear TS
            >> [-] @SS                    # clear SS
            <<<< [-] @ISENUM              # clear ISENUM
            > [-] @ISACT                  # clear ISACT
        <<<<< ] @N
        >>> . @ANY                        # any promoted action
        >>>>>>>>>> >>>>>>>>>> ~20 [-] @t5  # clear t5
    ] @t5
    <<<<<<<<<< <<<<<<<<<< <<<< ~24 [ @UNH  # unknown opcode
        # response header
        >>>>>>>>>> >>>>>>>>>> >> ~22 + @t3  # major
        . @t3                             # write t3
        [-] @t3                           # clear t3
        . @t3                             # write t3
        [-] @t3                           # clear t3
        <<<<<<<<<< <<<<<<<<<< <<<<<< ~26 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @OP  # copy OP to t3
        >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t4  # move t4 into OP
        < ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++ ~128 @t3  # opcode with the response bit
        . @t3                             # write t3
        [-] @t3                           # clear t3
        ++ @t3                            # status
        . @t3                             # write t3
        [-] @t3                           # clear t3
        . @t3                             # write t3
        [-] @t3                           # clear t3
        . @t3                             # write t3
        [-] @t3                           # clear t3
        <<<<<<<<<< <<<<<<<<<< <<<<< ~25 . @ID0  # echo request id
        > . @ID1                          # write ID1
        >> [-] @UNH                       # clear UNH
    ] @UNH
    >>>>>>>>>> >>>>>>>>>> > ~21 [-] @t2   # clear t2
] @t2
