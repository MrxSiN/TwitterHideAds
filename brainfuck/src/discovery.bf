# discovery bf: structural scoring thresholds profiles and limits
# Normative specification: docs/policy/discovery md ABI: docs/BRAINFUCK_ARCHITECTURE md
# Serves OP_RENDER_BOUNDARY OP_EXACT_BOUNDARY OP_VIDEO_BOUNDARY OP_VIDEO_SELECT
# OP_PROFILE and OP_LIMITS Scores are 16 bit little endian (LO HI)
#
# Tape layout
# %cell MAJ 0
# %cell OP 1
# %cell ID0 2
# %cell ID1 3
# %cell J 4
# %cell UNH 5
# %cell NP 6
# %cell R 7
# %cell NPC 8
# %cell FIRST 9
# %cell CF 10
# %cell TRAIL 11
# %cell BAD 12
# %cell MOD 13
# %cell LAY 14
# %cell PDEP 15
# %cell IDX0 16
# %cell ELIG 17
# %cell LO 18
# %cell HI 19
# %cell ACC 20
# %cell t0 21   scratch; zero whenever free
# %cell t1 22   scratch; zero whenever free
# %cell t2 23   scratch; zero whenever free
# %cell t3 24   scratch; zero whenever free
# %cell t4 25   scratch; zero whenever free
# %cell t5 26   scratch; zero whenever free
# %cell t6 27   scratch; zero whenever free
# %cell t7 28   scratch; zero whenever free
# %cell t8 29   scratch; zero whenever free
# %cell t9 30   scratch; zero whenever free
# %cell t10 31   scratch; zero whenever free
# %cell t11 32   scratch; zero whenever free
# %cell t12 33   scratch; zero whenever free
# %cell t13 34   scratch; zero whenever free
# %cell t14 35   scratch; zero whenever free
# %cell t15 36   scratch; zero whenever free
# %cell t16 37   scratch; zero whenever free
# %cell t17 38   scratch; zero whenever free
# %cell t18 39   scratch; zero whenever free
# %cell t19 40   scratch; zero whenever free
# %cell t20 41   scratch; zero whenever free
# %cell t21 42   scratch; zero whenever free
# %cell t22 43   scratch; zero whenever free
# %cell t23 44   scratch; zero whenever free
# %cell t24 45   scratch; zero whenever free
# %cell t25 46   scratch; zero whenever free
# %cell t26 47   scratch; zero whenever free
# %cell t27 48   scratch; zero whenever free
# %cell t28 49   scratch; zero whenever free
# %cell t29 50   scratch; zero whenever free
# %cell t30 51   scratch; zero whenever free
# %cell t31 52   scratch; zero whenever free
# %cell t32 53   scratch; zero whenever free
# %cell t33 54   scratch; zero whenever free
# %cell t34 55   scratch; zero whenever free
# %cell t35 56   scratch; zero whenever free
# %cell STATIC 57
# %cell ABS 58
# %cell NAT 59
# %cell SYN 60
# %cell VOID 61
# %cell DIRECT 62
# %cell IFACE 63
# %cell SHORT 64
# %cell BRIDGE 65
# %cell RVOID 66
# %cell RPRIM 67
# %cell FEQ 68
# %cell LISTLIKE 69
# %cell KX 70
# %cell DURT 71
# %cell RURT 72
# %cell PUB 73
# %cell NAMEEQ 74
# %cell FIRSTPOST 75
# %cell ANYC 76
# %cell NPOK 77
# %cell HASB 78
# %cell HASI 79
# %cell SKIP 80
# %cell T12 81
# %cell CH 82
# %cell VS 83
# %cell NVS 84
# %cell PID 85
# %cell M 86
# %cell HAS 87
# %cell B0S 88
# %cell B1S 89
# %cell HAS2 90
# %cell S0S 91
# %cell S1S 92
# %cell D 93

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
<<< [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @MAJ  # copy MAJ
>>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t1  # move t1 into MAJ
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
    <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @OP  # copy OP to t3
    >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t4  # move t4 into OP
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
    <<<<<<<<<< <<<<<<<<<< << ~22 . @ID0   # echo request id
    > . @ID1                              # write ID1
    >>>>>>>>>> >>>>>>>> ~18 [-] @t0       # clear t0
] @t0
>> [ @t2                                  # if equal
    <<<<<<<<<< <<<<<<<< ~18 [-] @UNH      # clear UNH
    + @UNH                                # UNH plus 1
    # opcode 48
    <<<< [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @OP  # copy OP
    >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t4  # move t4 into OP
    < ---------- ---------- ---------- ---------- -------- ~48 @t3  # subtract 48
    >> + @t5                              # assume equal
    << [ @t3                              # if different
        >> [-] @t5                        # not equal
        << [-] @t3                        # clear t3
    ] @t3
    >> [ @t5                              # if equal
        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @UNH  # clear UNH
        # OP RENDER BOUNDARY
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 , @STATIC  # read STATIC
        [ @STATIC                         # normalize STATIC
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30 [-] @STATIC  # clear STATIC
        ] @STATIC
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30] @t6  # move t6 into STATIC
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31 , @ABS  # read ABS
        [ @ABS                            # normalize ABS
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31 [-] @ABS  # clear ABS
        ] @ABS
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31] @t6  # move t6 into ABS
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~32 , @NAT  # read NAT
        [ @NAT                            # normalize NAT
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~32 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~32 [-] @NAT  # clear NAT
        ] @NAT
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~32 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~32+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~32] @t6  # move t6 into NAT
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33 , @SYN  # read SYN
        [ @SYN                            # normalize SYN
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33 [-] @SYN  # clear SYN
        ] @SYN
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33] @t6  # move t6 into SYN
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~34 , @VOID  # read VOID
        [ @VOID                           # normalize VOID
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~34 [-] @VOID  # clear VOID
        ] @VOID
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~34+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34] @t6  # move t6 into VOID
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~35 , @DIRECT  # read DIRECT
        [ @DIRECT                         # normalize DIRECT
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~35 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~35 [-] @DIRECT  # clear DIRECT
        ] @DIRECT
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~35 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~35+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~35] @t6  # move t6 into DIRECT
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>> ~36 , @IFACE  # read IFACE
        [ @IFACE                          # normalize IFACE
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~36 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>> ~36 [-] @IFACE  # clear IFACE
        ] @IFACE
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~36 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>> ~36+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~36] @t6  # move t6 into IFACE
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~37 , @SHORT  # read SHORT
        [ @SHORT                          # normalize SHORT
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~37 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~37 [-] @SHORT  # clear SHORT
        ] @SHORT
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~37 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~37+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~37] @t6  # move t6 into SHORT
        <<<<<<<<<< <<<<<<<<<< < ~21 , @NP  # parameter count
        >>>>>>>>>> ~10 [-] @IDX0          # clear IDX0
        + @IDX0                           # IDX0 plus 1
        <<<<<<<<<< ~10 [ @NP              # each parameter
            - @NP                         # NP minus 1
            > , @R                        # parameter role
            # first parameter
            >>>>>>>>> ~9 [- >>>>>>>>>> > ~11+ >+ <<<<<<<<<< << ~12] @IDX0  # copy IDX0
            >>>>>>>>>> >> ~12 [- <<<<<<<<<< << ~12+ >>>>>>>>>> >> ~12] @t7  # move t7 into IDX0
            < - @t6                       # subtract 1
            >> + @t8                      # assume equal
            << [ @t6                      # if different
                >> [-] @t8                # not equal
                # Composer not seen yet
                <<<<<<<<<< <<<<<<<<< ~19 [- >>>>>>>>>> >>>>>>>>>> ~20+ >+ <<<<<<<<<< <<<<<<<<<< < ~21] @CF  # copy CF
                >>>>>>>>>> >>>>>>>>>> > ~21 [- <<<<<<<<<< <<<<<<<<<< < ~21+ >>>>>>>>>> >>>>>>>>>> > ~21] @t10  # move t10 into CF
                > + @t11                  # assume equal
                << [ @t9                  # if different
                    >> [-] @t11           # not equal
                    <<<<<<<<<< <<<<<<<<<< < ~21 + @TRAIL  # TRAIL plus 1
                    # trailing mask is int
                    <<<< [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @R  # copy R
                    >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t13  # move t13 into R
                    < ----- @t12          # subtract 5
                    >> + @t14             # assume equal
                    << [ @t12             # if different
                        >> [-] @t14       # not equal
                        <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @BAD  # clear BAD
                        + @BAD            # BAD plus 1
                        >>>>>>>>>> >>>>>>>>>> > ~21 [-] @t12  # clear t12
                    ] @t12
                    >> [ @t14             # if equal
                        [-] @t14          # clear t14
                    ] @t14
                    <<<<< [-] @t9         # clear t9
                ] @t9
                >> [ @t11                 # if equal
                    # role before the Composer
                    # case R equals 1
                    <<<<<<<<<< <<<<<<<<<< <<<<< ~25 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @R  # copy R
                    >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t13  # move t13 into R
                    < - @t12              # subtract 1
                    >> + @t14             # assume equal
                    << [ @t12             # if different
                        >> [-] @t14       # not equal
                        << [-] @t12       # clear t12
                    ] @t12
                    >> [ @t14             # if equal
                        <<<<<<<<<< <<<<<<<<<< <<<<< ~25 [-] @CF  # clear CF
                        + @CF             # CF plus 1
                        >>>>>>>>>> >>>>>>>>>> >>>>> ~25 [-] @t14  # clear t14
                    ] @t14
                    # case R equals 2
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~28 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @R  # copy R
                    >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t13  # move t13 into R
                    < -- @t12             # subtract 2
                    >> + @t14             # assume equal
                    << [ @t12             # if different
                        >> [-] @t14       # not equal
                        << [-] @t12       # clear t12
                    ] @t12
                    >> [ @t14             # if equal
                        <<<<<<<<<< <<<<<<<<<< << ~22 [-] @MOD  # clear MOD
                        + @MOD            # MOD plus 1
                        >>>>>>>>>> >>>>>>>>>> >> ~22 [-] @t14  # clear t14
                    ] @t14
                    # case R equals 3
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~28 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @R  # copy R
                    >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t13  # move t13 into R
                    < --- @t12            # subtract 3
                    >> + @t14             # assume equal
                    << [ @t12             # if different
                        >> [-] @t14       # not equal
                        << [-] @t12       # clear t12
                    ] @t12
                    >> [ @t14             # if equal
                        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @LAY  # clear LAY
                        + @LAY            # LAY plus 1
                        >>>>>>>>>> >>>>>>>>>> > ~21 [-] @t14  # clear t14
                    ] @t14
                    # case R equals 4
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~28 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @R  # copy R
                    >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t13  # move t13 into R
                    < ---- @t12           # subtract 4
                    >> + @t14             # assume equal
                    << [ @t12             # if different
                        >> [-] @t14       # not equal
                        << [-] @t12       # clear t12
                    ] @t12
                    >> [ @t14             # if equal
                        <<<<<<<<<< <<<<<<<<<< ~20 [-] @PDEP  # clear PDEP
                        + @PDEP           # PDEP plus 1
                        >>>>>>>>>> >>>>>>>>>> ~20 [-] @t14  # clear t14
                    ] @t14
                    <<< [-] @t11          # clear t11
                ] @t11
                <<<<< [-] @t6             # clear t6
            ] @t6
            >> [ @t8                      # if equal
                # post model first
                <<<<<<<<<< <<<<<<<<<< << ~22 [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @R  # copy R
                >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t10  # move t10 into R
                < ---- @t9                # subtract 4
                >> + @t11                 # assume equal
                << [ @t9                  # if different
                    >> [-] @t11           # not equal
                    << [-] @t9            # clear t9
                ] @t9
                >> [ @t11                 # if equal
                    <<<<<<<<<< <<<<<<<<<< <<< ~23 [-] @FIRST  # clear FIRST
                    + @FIRST              # FIRST plus 1
                    >>>>>>>>>> >>>>>>>>>> >>> ~23 [-] @t11  # clear t11
                ] @t11
                <<<<<<<<<< <<<<<< ~16 [-] @IDX0  # clear IDX0
                >>>>>>>>>> >>> ~13 [-] @t8  # clear t8
            ] @t8
            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @R  # clear R
        < ] @NP
        >>>>>>>>>> ~10 [-] @IDX0          # clear IDX0
        # one or two trailing masks
        # case TRAIL equals 1
        <<<<< [- >>>>>>>>>> >>>>>> ~16+ >+ <<<<<<<<<< <<<<<<< ~17] @TRAIL  # copy TRAIL
        >>>>>>>>>> >>>>>>> ~17 [- <<<<<<<<<< <<<<<<< ~17+ >>>>>>>>>> >>>>>>> ~17] @t7  # move t7 into TRAIL
        < - @t6                           # subtract 1
        >> + @t8                          # assume equal
        << [ @t6                          # if different
            >> [-] @t8                    # not equal
            << [-] @t6                    # clear t6
        ] @t6
        >> [ @t8                          # if equal
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @T12  # clear T12
            + @T12                        # T12 plus 1
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t8  # clear t8
        ] @t8
        # case TRAIL equals 2
        <<<<<<<<<< <<<<<<<< ~18 [- >>>>>>>>>> >>>>>> ~16+ >+ <<<<<<<<<< <<<<<<< ~17] @TRAIL  # copy TRAIL
        >>>>>>>>>> >>>>>>> ~17 [- <<<<<<<<<< <<<<<<< ~17+ >>>>>>>>>> >>>>>>> ~17] @t7  # move t7 into TRAIL
        < -- @t6                          # subtract 2
        >> + @t8                          # assume equal
        << [ @t6                          # if different
            >> [-] @t8                    # not equal
            << [-] @t6                    # clear t6
        ] @t6
        >> [ @t8                          # if equal
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @T12  # clear T12
            + @T12                        # T12 plus 1
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t8  # clear t8
        ] @t8
        >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~28 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~29] @STATIC  # copy STATIC to t6
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~29 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~29+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~29] @t7  # move t7 into STATIC
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33] @VOID  # copy VOID to t6
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33] @t7  # move t7 into VOID
        <<<<<<<<<< <<<<<<<<< ~19 [- >>>>>>>>>> >>>>>>>> ~18+ >+ <<<<<<<<<< <<<<<<<<< ~19] @FIRST  # copy FIRST to t6
        >>>>>>>>>> >>>>>>>>> ~19 [- <<<<<<<<<< <<<<<<<<< ~19+ >>>>>>>>>> >>>>>>>>> ~19] @t7  # move t7 into FIRST
        <<<<<<<<<< <<<<<<<< ~18 [- >>>>>>>>>> >>>>>>> ~17+ >+ <<<<<<<<<< <<<<<<<< ~18] @CF  # copy CF to t6
        >>>>>>>>>> >>>>>>>> ~18 [- <<<<<<<<<< <<<<<<<< ~18+ >>>>>>>>>> >>>>>>>> ~18] @t7  # move t7 into CF
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~54+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53] @T12  # copy T12 to t6
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~53 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~53] @t7  # move t7 into T12
        < + @t6                           # t6 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~29] @ABS  # copy ABS to t7
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~29 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~29+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~29] @t8  # move t8 into ABS
        < [- <- >] @t7                    # count ABS absent
        < + @t6                           # t6 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~32 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30] @NAT  # copy NAT to t7
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30] @t8  # move t8 into NAT
        < [- <- >] @t7                    # count NAT absent
        < + @t6                           # t6 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~32+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31] @SYN  # copy SYN to t7
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31] @t8  # move t8 into SYN
        < [- <- >] @t7                    # count SYN absent
        < + @t6                           # t6 plus 1
        <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>>>>> ~16+ >+ <<<<<<<<<< <<<<<<< ~17] @BAD  # copy BAD to t7
        >>>>>>>>>> >>>>>>> ~17 [- <<<<<<<<<< <<<<<<< ~17+ >>>>>>>>>> >>>>>>> ~17] @t8  # move t8 into BAD
        < [- <- >] @t7                    # count BAD absent
        # all conditions hold
        < [- >+ >+ <<] @t6                # copy t6
        >> [- <<+ >>] @t8                 # move t8 into t6
        < --------- ~9 @t7                # subtract 9
        >> + @t9                          # assume equal
        << [ @t7                          # if different
            >> [-] @t9                    # not equal
            << [-] @t7                    # clear t7
        ] @t7
        >> [ @t9                          # if equal
            <<<<<<<<<< <<< ~13 [-] @ELIG  # clear ELIG
            + @ELIG                       # ELIG plus 1
            >>>>>>>>>> >>> ~13 [-] @t9    # clear t9
        ] @t9
        <<< [-] @t6                       # clear t6
        <<<<<<<<<< ~10 [- >>>>>>>>>> ~10+ >+ <<<<<<<<<< < ~11] @ELIG  # copy ELIG to t6
        >>>>>>>>>> > ~11 [- <<<<<<<<<< < ~11+ >>>>>>>>>> > ~11] @t7  # move t7 into ELIG
        < [ @t6                           # eligible boundaries are scored
            # base: static 25 void 25 Composer 60 masks 25 model package 80
            <<<<<<<<< ~9 [-] @LO          # LO is 215
            ---------- ---------- ---------- ---------- - ~41 @LO  # LO minus 41
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~44 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33] @DIRECT  # copy DIRECT to t7
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33] @t8  # move t8 into DIRECT
            + @t8                         # t8 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~32+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31] @DIRECT  # copy DIRECT to t9
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31] @t10  # move t10 into DIRECT
            < [ @t9                       # if t9 then
                < [-] @t8                 # clear t8
                > [-] @t9                 # clear t9
            ] @t9
            < [ @t8                       # flag DIRECT absent
                > [-] @t9                 # add 25
                ++++++++++ ++++++++++ +++++ ~25 @t9  # t9 plus 25
                [ @t9                     # while t9
                    - @t9                 # t9 minus 1
                    <<<<<<<<<< << ~12 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @LO  # copy LO
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t11  # move t11 into LO
                    > + @t12              # assume equal
                    << [ @t10             # if different
                        >> [-] @t12       # not equal
                        << [-] @t10       # clear t10
                    ] @t10
                    >> [ @t12             # if equal
                        <<<<<<<<<< <<<< ~14 + @HI  # HI plus 1
                        >>>>>>>>>> >>>> ~14 [-] @t12  # clear t12
                    ] @t12
                <<< ] @t9
                < [-] @t8                 # clear t8
            ] @t8
            < [ @t7                       # flag DIRECT
                > [-] @t8                 # add 80
                ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ~80 @t8  # t8 plus 80
                [ @t8                     # while t8
                    - @t8                 # t8 minus 1
                    <<<<<<<<<< < ~11 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >> ~12+ >+ <<<<<<<<<< <<< ~13] @LO  # copy LO
                    >>>>>>>>>> >>> ~13 [- <<<<<<<<<< <<< ~13+ >>>>>>>>>> >>> ~13] @t10  # move t10 into LO
                    > + @t11              # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        <<<<<<<<<< <<< ~13 + @HI  # HI plus 1
                        >>>>>>>>>> >>> ~13 [-] @t11  # clear t11
                    ] @t11
                <<< ] @t8
                < [-] @t7                 # clear t7
            ] @t7
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~35 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~35+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~34] @IFACE  # copy IFACE to t7
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~34+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34] @t8  # move t8 into IFACE
            < [ @t7                       # flag IFACE
                > [-] @t8                 # add 15
                ++++++++++ +++++ ~15 @t8  # t8 plus 15
                [ @t8                     # while t8
                    - @t8                 # t8 minus 1
                    <<<<<<<<<< < ~11 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >> ~12+ >+ <<<<<<<<<< <<< ~13] @LO  # copy LO
                    >>>>>>>>>> >>> ~13 [- <<<<<<<<<< <<< ~13+ >>>>>>>>>> >>> ~13] @t10  # move t10 into LO
                    > + @t11              # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        <<<<<<<<<< <<< ~13 + @HI  # HI plus 1
                        >>>>>>>>>> >>> ~13 [-] @t11  # clear t11
                    ] @t11
                <<< ] @t8
                < [-] @t7                 # clear t7
            ] @t7
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>> ~36 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~36+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~35] @SHORT  # copy SHORT to t7
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~35 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~35+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~35] @t8  # move t8 into SHORT
            < [ @t7                       # flag SHORT
                > [-] @t8                 # add 10
                ++++++++++ ~10 @t8        # t8 plus 10
                [ @t8                     # while t8
                    - @t8                 # t8 minus 1
                    <<<<<<<<<< < ~11 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >> ~12+ >+ <<<<<<<<<< <<< ~13] @LO  # copy LO
                    >>>>>>>>>> >>> ~13 [- <<<<<<<<<< <<< ~13+ >>>>>>>>>> >>> ~13] @t10  # move t10 into LO
                    > + @t11              # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        <<<<<<<<<< <<< ~13 + @HI  # HI plus 1
                        >>>>>>>>>> >>> ~13 [-] @t11  # clear t11
                    ] @t11
                <<< ] @t8
                < [-] @t7                 # clear t7
            ] @t7
            <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>>>> ~15+ >+ <<<<<<<<<< <<<<<< ~16] @MOD  # copy MOD to t7
            >>>>>>>>>> >>>>>> ~16 [- <<<<<<<<<< <<<<<< ~16+ >>>>>>>>>> >>>>>> ~16] @t8  # move t8 into MOD
            < [ @t7                       # flag MOD
                > [-] @t8                 # add 35
                ++++++++++ ++++++++++ ++++++++++ +++++ ~35 @t8  # t8 plus 35
                [ @t8                     # while t8
                    - @t8                 # t8 minus 1
                    <<<<<<<<<< < ~11 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >> ~12+ >+ <<<<<<<<<< <<< ~13] @LO  # copy LO
                    >>>>>>>>>> >>> ~13 [- <<<<<<<<<< <<< ~13+ >>>>>>>>>> >>> ~13] @t10  # move t10 into LO
                    > + @t11              # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        <<<<<<<<<< <<< ~13 + @HI  # HI plus 1
                        >>>>>>>>>> >>> ~13 [-] @t11  # clear t11
                    ] @t11
                <<< ] @t8
                < [-] @t7                 # clear t7
            ] @t7
            <<<<<<<<<< <<<< ~14 [- >>>>>>>>>> >>>> ~14+ >+ <<<<<<<<<< <<<<< ~15] @LAY  # copy LAY to t7
            >>>>>>>>>> >>>>> ~15 [- <<<<<<<<<< <<<<< ~15+ >>>>>>>>>> >>>>> ~15] @t8  # move t8 into LAY
            < [ @t7                       # flag LAY
                > [-] @t8                 # add 35
                ++++++++++ ++++++++++ ++++++++++ +++++ ~35 @t8  # t8 plus 35
                [ @t8                     # while t8
                    - @t8                 # t8 minus 1
                    <<<<<<<<<< < ~11 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >> ~12+ >+ <<<<<<<<<< <<< ~13] @LO  # copy LO
                    >>>>>>>>>> >>> ~13 [- <<<<<<<<<< <<< ~13+ >>>>>>>>>> >>> ~13] @t10  # move t10 into LO
                    > + @t11              # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        <<<<<<<<<< <<< ~13 + @HI  # HI plus 1
                        >>>>>>>>>> >>> ~13 [-] @t11  # clear t11
                    ] @t11
                <<< ] @t8
                < [-] @t7                 # clear t7
            ] @t7
            <<<<<<<<<< <<< ~13 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @PDEP  # copy PDEP to t7
            >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t8  # move t8 into PDEP
            < [ @t7                       # flag PDEP
                > [-] @t8                 # add 20
                ++++++++++ ++++++++++ ~20 @t8  # t8 plus 20
                [ @t8                     # while t8
                    - @t8                 # t8 minus 1
                    <<<<<<<<<< < ~11 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >> ~12+ >+ <<<<<<<<<< <<< ~13] @LO  # copy LO
                    >>>>>>>>>> >>> ~13 [- <<<<<<<<<< <<< ~13+ >>>>>>>>>> >>> ~13] @t10  # move t10 into LO
                    > + @t11              # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        <<<<<<<<<< <<< ~13 + @HI  # HI plus 1
                        >>>>>>>>>> >>> ~13 [-] @t11  # clear t11
                    ] @t11
                <<< ] @t8
                < [-] @t7                 # clear t7
            ] @t7
            < [-] @t6                     # clear t6
        ] @t6
        [-] @t6                           # t6 is 1
        + @t6                             # t6 plus 1
        > [-] @t7                         # t7 is 63
        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++ ~63 @t7  # t7 plus 63
        <<<<<<<<< ~9 [- >>>>>>>>>> >>>> ~14+ >>+ <<<<<<<<<< <<<<<< ~16] @HI  # copy HI to t12
        >>>>>>>>>> >>>>>> ~16 [- <<<<<<<<<< <<<<<< ~16+ >>>>>>>>>> >>>>>> ~16] @t14  # move t14 into HI
        <<<<<<<< ~8 [- >>>>>>>+ >+ <<<<<<<< ~8] @t6  # copy t6 to t13
        >>>>>>>> ~8 [- <<<<<<<< ~8+ >>>>>>>> ~8] @t14  # move t14 into t6
        << [ @t12                         # count down
            # case t13 equals 0
            > [- >+ >+ <<] @t13           # copy t13
            >> [- <<+ >>] @t15            # move t15 into t13
            > + @t16                      # assume equal
            << [ @t14                     # if different
                >> [-] @t16               # not equal
                <<<< - @t12               # t12 minus 1
                > - @t13                  # t13 minus 1
                > [-] @t14                # clear t14
            ] @t14
            >> [ @t16                     # if equal
                <<<<<<< [-] @t9           # clear t9
                + @t9                     # t9 plus 1
                >>> [-] @t12              # clear t12
                >>>> [-] @t16             # clear t16
            ] @t16
        <<<< ] @t12
        > [-] @t13                        # clear t13
        <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>>> ~14+ >>+ <<<<<<<<<< <<<<<< ~16] @HI  # copy HI to t12
        >>>>>>>>>> >>>>>> ~16 [- <<<<<<<<<< <<<<<< ~16+ >>>>>>>>>> >>>>>> ~16] @t14  # move t14 into HI
        <<<<<<<< ~8 [- >>>>>>>+ >+ <<<<<<<< ~8] @t6  # copy t6 to t13
        >>>>>>>> ~8 [- <<<<<<<< ~8+ >>>>>>>> ~8] @t14  # move t14 into t6
        < [- <- >] @t13                   # subtract
        <<< + @t10                        # t10 plus 1
        >> [ @t12                         # if t12 then
            << [-] @t10                   # clear t10
            >> [-] @t12                   # clear t12
            [-] @t12                      # clear t12
        ] @t12
        <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>>>> ~15+ >>+ <<<<<<<<<< <<<<<<< ~17] @LO  # copy LO to t12
        >>>>>>>>>> >>>>>>> ~17 [- <<<<<<<<<< <<<<<<< ~17+ >>>>>>>>>> >>>>>>> ~17] @t14  # move t14 into LO
        <<<<<<< [- >>>>>>+ >+ <<<<<<<] @t7  # copy t7 to t13
        >>>>>>> [- <<<<<<<+ >>>>>>>] @t14  # move t14 into t7
        << [ @t12                         # count down
            # case t13 equals 0
            > [- >+ >+ <<] @t13           # copy t13
            >> [- <<+ >>] @t15            # move t15 into t13
            > + @t16                      # assume equal
            << [ @t14                     # if different
                >> [-] @t16               # not equal
                <<<< - @t12               # t12 minus 1
                > - @t13                  # t13 minus 1
                > [-] @t14                # clear t14
            ] @t14
            >> [ @t16                     # if equal
                <<<<< [-] @t11            # clear t11
                + @t11                    # t11 plus 1
                > [-] @t12                # clear t12
                >>>> [-] @t16             # clear t16
            ] @t16
        <<<< ] @t12
        > [-] @t13                        # clear t13
        <<< [- >>+ <<] @t10               # move t10 into t12
        > [- >+ <] @t11                   # move t11 into t12
        # high equal and low greater
        > [- >+ >+ <<] @t12               # copy t12
        >> [- <<+ >>] @t14                # move t14 into t12
        < -- @t13                         # subtract 2
        >> + @t15                         # assume equal
        << [ @t13                         # if different
            >> [-] @t15                   # not equal
            << [-] @t13                   # clear t13
        ] @t13
        >> [ @t15                         # if equal
            <<<<<< + @t9                  # t9 plus 1
            >>>>>> [-] @t15               # clear t15
        ] @t15
        <<< [-] @t12                      # clear t12
        <<< [ @t9                         # if t9 then
            < [-] @t8                     # clear t8
            + @t8                         # t8 plus 1
            > [-] @t9                     # clear t9
        ] @t9
        <<< [-] @t6                       # clear t6
        > [-] @t7                         # clear t7
        <<<<<<<<<< < ~11 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @ELIG  # copy ELIG to t9
        >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t10  # move t10 into ELIG
        << [- >+ >+ <<] @t8               # copy t8 to t9
        >> [- <<+ >>] @t10                # move t10 into t8
        # all conditions hold
        < [- >+ >+ <<] @t9                # copy t9
        >> [- <<+ >>] @t11                # move t11 into t9
        < -- @t10                         # subtract 2
        >> + @t12                         # assume equal
        << [ @t10                         # if different
            >> [-] @t12                   # not equal
            << [-] @t10                   # clear t10
        ] @t10
        >> [ @t12                         # if equal
            <<<<<<<<<< <<< ~13 [-] @ACC   # clear ACC
            + @ACC                        # ACC plus 1
            >>>>>>>>>> >>> ~13 [-] @t12   # clear t12
        ] @t12
        <<< [-] @t9                       # clear t9
        < [-] @t8                         # clear t8
        # response header
        << + @t6                          # major
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<<< ~26 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @OP  # copy OP to t6
        >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t7  # move t7 into OP
        < ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++ ~128 @t6  # opcode with the response bit
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        ++++ @t6                          # payload length low
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<< ~25 . @ID0  # echo request id
        > . @ID1                          # write ID1
        >>>>>>>>>> >>>> ~14 . @ELIG       # write ELIG
        > . @LO                           # write LO
        > . @HI                           # write HI
        > . @ACC                          # write ACC
        >>>>>> [-] @t5                    # clear t5
    ] @t5
    # opcode 49
    <<<<<<<<<< <<<<<<<<<< <<<<< ~25 [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @OP  # copy OP
    >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t4  # move t4 into OP
    < ---------- ---------- ---------- ---------- --------- ~49 @t3  # subtract 49
    >> + @t5                              # assume equal
    << [ @t3                              # if different
        >> [-] @t5                        # not equal
        << [-] @t3                        # clear t3
    ] @t3
    >> [ @t5                              # if equal
        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @UNH  # clear UNH
        # OP EXACT BOUNDARY
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~69 , @NAMEEQ  # read NAMEEQ
        [ @NAMEEQ                         # normalize NAMEEQ
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~47 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~47 [-] @NAMEEQ  # clear NAMEEQ
        ] @NAMEEQ
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~47 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~47+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~47] @t6  # move t6 into NAMEEQ
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31 , @ABS  # read ABS
        [ @ABS                            # normalize ABS
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31 [-] @ABS  # clear ABS
        ] @ABS
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31] @t6  # move t6 into ABS
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~32 , @NAT  # read NAT
        [ @NAT                            # normalize NAT
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~32 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~32 [-] @NAT  # clear NAT
        ] @NAT
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~32 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~32+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~32] @t6  # move t6 into NAT
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33 , @SYN  # read SYN
        [ @SYN                            # normalize SYN
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33 [-] @SYN  # clear SYN
        ] @SYN
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33] @t6  # move t6 into SYN
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~34 , @VOID  # read VOID
        [ @VOID                           # normalize VOID
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~34 [-] @VOID  # clear VOID
        ] @VOID
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~34+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34] @t6  # move t6 into VOID
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~48 , @FIRSTPOST  # read FIRSTPOST
        [ @FIRSTPOST                      # normalize FIRSTPOST
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~48 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~48 [-] @FIRSTPOST  # clear FIRSTPOST
        ] @FIRSTPOST
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~48 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~48+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~48] @t6  # move t6 into FIRSTPOST
        <<<<<<<<<< <<<<<<<<<< < ~21 , @NP  # parameter count
        [- >>+ >>>>>>>>>> >>>>>>>>> ~19+ <<<<<<<<<< <<<<<<<<<< < ~21] @NP  # copy NP to NPC
        >>>>>>>>>> >>>>>>>>>> > ~21 [- <<<<<<<<<< <<<<<<<<<< < ~21+ >>>>>>>>>> >>>>>>>>>> > ~21] @t6  # move t6 into NP
        <<<<<<<<<< <<<<<<<<<< < ~21 [ @NP  # each parameter
            - @NP                         # NP minus 1
            > , @R                        # read R
            # Composer parameter
            [- >>>>>>>>>> >>>>>>>>>> ~20+ >+ <<<<<<<<<< <<<<<<<<<< < ~21] @R  # copy R
            >>>>>>>>>> >>>>>>>>>> > ~21 [- <<<<<<<<<< <<<<<<<<<< < ~21+ >>>>>>>>>> >>>>>>>>>> > ~21] @t7  # move t7 into R
            < - @t6                       # subtract 1
            >> + @t8                      # assume equal
            << [ @t6                      # if different
                >> [-] @t8                # not equal
                << [-] @t6                # clear t6
            ] @t6
            >> [ @t8                      # if equal
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~47 [-] @ANYC  # clear ANYC
                + @ANYC                   # ANYC plus 1
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~47 [-] @t8  # clear t8
            ] @t8
            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @R  # clear R
        < ] @NP
        >>>>>>>>>> >>>>>>>>>> > ~21 [-] @t6  # t6 is 10
        ++++++++++ ~10 @t6                # t6 plus 10
        <<<<<<<<<< <<<<<<<<< ~19 [- >>>>>>>>>> >>>>>>>>>> >> ~22+ >>+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @NPC  # copy NPC to t9
        >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t11  # move t11 into NPC
        <<<<< [- >>>>+ >+ <<<<<] @t6      # copy t6 to t10
        >>>>> [- <<<<<+ >>>>>] @t11       # move t11 into t6
        << [ @t9                          # count down
            # case t10 equals 0
            > [- >+ >+ <<] @t10           # copy t10
            >> [- <<+ >>] @t12            # move t12 into t10
            > + @t13                      # assume equal
            << [ @t11                     # if different
                >> [-] @t13               # not equal
                <<<< - @t9                # t9 minus 1
                > - @t10                  # t10 minus 1
                > [-] @t11                # clear t11
            ] @t11
            >> [ @t13                     # if equal
                <<<<<< [-] @t7            # clear t7
                + @t7                     # t7 plus 1
                >> [-] @t9                # clear t9
                >>>> [-] @t13             # clear t13
            ] @t13
        <<<< ] @t9
        > [-] @t10                        # clear t10
        <<<< [-] @t6                      # clear t6
        <<<<<<<<<< <<<<<<<<< ~19 [- >>>>>>>>>> >>>>>>>>>> >> ~22+ >+ <<<<<<<<<< <<<<<<<<<< <<< ~23] @NPC  # copy NPC to t9
        >>>>>>>>>> >>>>>>>>>> >>> ~23 [- <<<<<<<<<< <<<<<<<<<< <<< ~23+ >>>>>>>>>> >>>>>>>>>> >>> ~23] @t10  # move t10 into NPC
        < [ @t9                           # if t9 then
            < [-] @t8                     # clear t8
            + @t8                         # t8 plus 1
            > [-] @t9                     # clear t9
        ] @t9
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~44 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~44+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~43] @NAMEEQ  # copy NAMEEQ to t9
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~43 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~43+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~43] @t10  # move t10 into NAMEEQ
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30] @VOID  # copy VOID to t9
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30] @t10  # move t10 into VOID
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~44 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~45+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~44] @FIRSTPOST  # copy FIRSTPOST to t9
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~44 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~44+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~44] @t10  # move t10 into FIRSTPOST
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~45 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~46+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~45] @ANYC  # copy ANYC to t9
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~45 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~45+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~45] @t10  # move t10 into ANYC
        << [- >+ >+ <<] @t8               # copy t8 to t9
        >> [- <<+ >>] @t10                # move t10 into t8
        < + @t9                           # t9 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~28 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>> ~26] @ABS  # copy ABS to t10
        <<<<<<<<<< <<<<<<<<<< <<<<<< ~26 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ <<<<<<<<<< <<<<<<<<<< <<<<<< ~26] @t11  # move t11 into ABS
        < [- <- >] @t10                   # count ABS absent
        < + @t9                           # t9 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~29 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~28+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @NAT  # copy NAT to t10
        <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27 [- >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @t11  # move t11 into NAT
        < [- <- >] @t10                   # count NAT absent
        < + @t9                           # t9 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~29+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~28] @SYN  # copy SYN to t10
        <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~28 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~28+ <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~28] @t11  # move t11 into SYN
        < [- <- >] @t10                   # count SYN absent
        < + @t9                           # t9 plus 1
        << [- >>>+ >+ <<<<] @t7           # copy t7 to t10
        >>>> [- <<<<+ >>>>] @t11          # move t11 into t7
        < [- <- >] @t10                   # count t7 absent
        # all conditions hold
        < [- >+ >+ <<] @t9                # copy t9
        >> [- <<+ >>] @t11                # move t11 into t9
        < --------- ~9 @t10               # subtract 9
        >> + @t12                         # assume equal
        << [ @t10                         # if different
            >> [-] @t12                   # not equal
            << [-] @t10                   # clear t10
        ] @t10
        >> [ @t12                         # if equal
            <<<<<<<<<< <<< ~13 [-] @ACC   # clear ACC
            + @ACC                        # ACC plus 1
            >>>>>>>>>> >>> ~13 [-] @t12   # clear t12
        ] @t12
        <<< [-] @t9                       # clear t9
        << [-] @t7                        # clear t7
        > [-] @t8                         # clear t8
        # response header
        << + @t6                          # major
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<<< ~26 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @OP  # copy OP to t6
        >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t7  # move t7 into OP
        < ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++ ~128 @t6  # opcode with the response bit
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        + @t6                             # payload length low
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<< ~25 . @ID0  # echo request id
        > . @ID1                          # write ID1
        >>>>>>>>>> >>>>>>> ~17 . @ACC     # write ACC
        >>>>>> [-] @t5                    # clear t5
    ] @t5
    # opcode 50
    <<<<<<<<<< <<<<<<<<<< <<<<< ~25 [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @OP  # copy OP
    >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t4  # move t4 into OP
    < ---------- ---------- ---------- ---------- ---------- ~50 @t3  # subtract 50
    >> + @t5                              # assume equal
    << [ @t3                              # if different
        >> [-] @t5                        # not equal
        << [-] @t3                        # clear t3
    ] @t3
    >> [ @t5                              # if equal
        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @UNH  # clear UNH
        # OP VIDEO BOUNDARY
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 , @STATIC  # read STATIC
        [ @STATIC                         # normalize STATIC
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30 [-] @STATIC  # clear STATIC
        ] @STATIC
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~30] @t6  # move t6 into STATIC
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31 , @ABS  # read ABS
        [ @ABS                            # normalize ABS
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31 [-] @ABS  # clear ABS
        ] @ABS
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~31+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~31] @t6  # move t6 into ABS
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~32 , @NAT  # read NAT
        [ @NAT                            # normalize NAT
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~32 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~32 [-] @NAT  # clear NAT
        ] @NAT
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~32 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~32+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~32] @t6  # move t6 into NAT
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33 , @SYN  # read SYN
        [ @SYN                            # normalize SYN
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33 [-] @SYN  # clear SYN
        ] @SYN
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33] @t6  # move t6 into SYN
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~38 , @BRIDGE  # read BRIDGE
        [ @BRIDGE                         # normalize BRIDGE
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~38 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~38 [-] @BRIDGE  # clear BRIDGE
        ] @BRIDGE
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~38 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~38+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~38] @t6  # move t6 into BRIDGE
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~39 , @RVOID  # read RVOID
        [ @RVOID                          # normalize RVOID
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~39 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~39 [-] @RVOID  # clear RVOID
        ] @RVOID
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~39 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~39+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~39] @t6  # move t6 into RVOID
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~40 , @RPRIM  # read RPRIM
        [ @RPRIM                          # normalize RPRIM
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~40 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~40 [-] @RPRIM  # clear RPRIM
        ] @RPRIM
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~40 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~40+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~40] @t6  # move t6 into RPRIM
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~41 , @FEQ  # read FEQ
        [ @FEQ                            # normalize FEQ
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~41 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~41 [-] @FEQ  # clear FEQ
        ] @FEQ
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~41 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~41+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~41] @t6  # move t6 into FEQ
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~42 , @LISTLIKE  # read LISTLIKE
        [ @LISTLIKE                       # normalize LISTLIKE
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~42 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~42 [-] @LISTLIKE  # clear LISTLIKE
        ] @LISTLIKE
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~42 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~42+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~42] @t6  # move t6 into LISTLIKE
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~43 , @KX  # read KX
        [ @KX                             # normalize KX
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~43 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~43 [-] @KX  # clear KX
        ] @KX
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~43 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~43+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~43] @t6  # move t6 into KX
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~44 , @DURT  # read DURT
        [ @DURT                           # normalize DURT
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~44 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~44 [-] @DURT  # clear DURT
        ] @DURT
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~44 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~44+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~44] @t6  # move t6 into DURT
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~45 , @RURT  # read RURT
        [ @RURT                           # normalize RURT
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~45 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~45 [-] @RURT  # clear RURT
        ] @RURT
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~45 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~45+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~45] @t6  # move t6 into RURT
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~37 , @SHORT  # read SHORT
        [ @SHORT                          # normalize SHORT
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~37 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~37 [-] @SHORT  # clear SHORT
        ] @SHORT
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~37 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~37+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~37] @t6  # move t6 into SHORT
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>> ~46 , @PUB  # read PUB
        [ @PUB                            # normalize PUB
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~46 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>> ~46 [-] @PUB  # clear PUB
        ] @PUB
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~46 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>> ~46+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~46] @t6  # move t6 into PUB
        <<<<<<<<<< <<<<<<<<<< < ~21 , @NP  # parameter count
        [- >>+ >>>>>>>>>> >>>>>>>>> ~19+ <<<<<<<<<< <<<<<<<<<< < ~21] @NP  # copy NP to NPC
        >>>>>>>>>> >>>>>>>>>> > ~21 [- <<<<<<<<<< <<<<<<<<<< < ~21+ >>>>>>>>>> >>>>>>>>>> > ~21] @t6  # move t6 into NP
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [-] @SKIP  # the first two parameters are the state and the list
        ++ @SKIP                          # SKIP plus 2
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~74 [ @NP  # each parameter
            - @NP                         # NP minus 1
            > , @R                        # read R
            # past the first two
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~73 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~53+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52] @SKIP  # copy SKIP
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52] @t7  # move t7 into SKIP
            > + @t8                       # assume equal
            << [ @t6                      # if different
                >> [-] @t8                # not equal
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51 - @SKIP  # SKIP minus 1
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~53 [-] @t6  # clear t6
            ] @t6
            >> [ @t8                      # if equal
                # flag parameter
                # case R equals 1
                <<<<<<<<<< <<<<<<<<<< << ~22 [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @R  # copy R
                >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t10  # move t10 into R
                < - @t9                   # subtract 1
                >> + @t11                 # assume equal
                << [ @t9                  # if different
                    >> [-] @t11           # not equal
                    << [-] @t9            # clear t9
                ] @t9
                >> [ @t11                 # if equal
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>> ~46 [-] @HASB  # clear HASB
                    + @HASB               # HASB plus 1
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~46 [-] @t11  # clear t11
                ] @t11
                # case R equals 2
                <<<<<<<<<< <<<<<<<<<< <<<<< ~25 [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @R  # copy R
                >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t10  # move t10 into R
                < -- @t9                  # subtract 2
                >> + @t11                 # assume equal
                << [ @t9                  # if different
                    >> [-] @t11           # not equal
                    << [-] @t9            # clear t9
                ] @t9
                >> [ @t11                 # if equal
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~47 [-] @HASI  # clear HASI
                    + @HASI               # HASI plus 1
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~47 [-] @t11  # clear t11
                ] @t11
                <<< [-] @t8               # clear t8
            ] @t8
            <<<<<<<<<< <<<<<<<<<< << ~22 [-] @R  # clear R
        < ] @NP
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~74 [-] @SKIP  # clear SKIP
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~53 [-] @t6  # t6 is 4
        ++++ @t6                          # t6 plus 4
        [- >>>+ >>+ <<<<<] @t6            # copy t6 to t9
        >>>>> [- <<<<<+ >>>>>] @t11       # move t11 into t6
        <<<<<<<<<< <<<<<<<<<< <<<< ~24 [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @NPC  # copy NPC to t10
        >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t11  # move t11 into NPC
        << [ @t9                          # count down
            # case t10 equals 0
            > [- >+ >+ <<] @t10           # copy t10
            >> [- <<+ >>] @t12            # move t12 into t10
            > + @t13                      # assume equal
            << [ @t11                     # if different
                >> [-] @t13               # not equal
                <<<< - @t9                # t9 minus 1
                > - @t10                  # t10 minus 1
                > [-] @t11                # clear t11
            ] @t11
            >> [ @t13                     # if equal
                <<<<<< [-] @t7            # clear t7
                + @t7                     # t7 plus 1
                >> [-] @t9                # clear t9
                >>>> [-] @t13             # clear t13
            ] @t13
        <<<< ] @t9
        > [-] @t10                        # clear t10
        <<<< [-] @t6                      # t6 is 8
        ++++++++ ~8 @t6                   # t6 plus 8
        <<<<<<<<<< <<<<<<<<< ~19 [- >>>>>>>>>> >>>>>>>>>> >> ~22+ >>+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @NPC  # copy NPC to t9
        >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t11  # move t11 into NPC
        <<<<< [- >>>>+ >+ <<<<<] @t6      # copy t6 to t10
        >>>>> [- <<<<<+ >>>>>] @t11       # move t11 into t6
        << [ @t9                          # count down
            # case t10 equals 0
            > [- >+ >+ <<] @t10           # copy t10
            >> [- <<+ >>] @t12            # move t12 into t10
            > + @t13                      # assume equal
            << [ @t11                     # if different
                >> [-] @t13               # not equal
                <<<< - @t9                # t9 minus 1
                > - @t10                  # t10 minus 1
                > [-] @t11                # clear t11
            ] @t11
            >> [ @t13                     # if equal
                <<<<< [-] @t8             # clear t8
                + @t8                     # t8 plus 1
                > [-] @t9                 # clear t9
                >>>> [-] @t13             # clear t13
            ] @t13
        <<<< ] @t9
        > [-] @t10                        # clear t10
        <<<< [-] @t6                      # clear t6
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>> ~26] @STATIC  # copy STATIC to t9
        <<<<<<<<<< <<<<<<<<<< <<<<<< ~26 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ <<<<<<<<<< <<<<<<<<<< <<<<<< ~26] @t10  # move t10 into STATIC
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~37 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~38+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~37] @FEQ  # copy FEQ to t9
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~37 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~37+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~37] @t10  # move t10 into FEQ
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~38 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~39+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~38] @LISTLIKE  # copy LISTLIKE to t9
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~38 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~38+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~38] @t10  # move t10 into LISTLIKE
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~40 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~41+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~40] @DURT  # copy DURT to t9
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~40 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~40+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~40] @t10  # move t10 into DURT
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~41 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~42+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~41] @RURT  # copy RURT to t9
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~41 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~41+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~41] @t10  # move t10 into RURT
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~47 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~48+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~47] @HASB  # copy HASB to t9
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~47 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~47+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~47] @t10  # move t10 into HASB
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~48 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~49+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~48] @HASI  # copy HASI to t9
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~48 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~48+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~48] @t10  # move t10 into HASI
        < + @t9                           # t9 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~28 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>> ~26] @ABS  # copy ABS to t10
        <<<<<<<<<< <<<<<<<<<< <<<<<< ~26 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ <<<<<<<<<< <<<<<<<<<< <<<<<< ~26] @t11  # move t11 into ABS
        < [- <- >] @t10                   # count ABS absent
        < + @t9                           # t9 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~29 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~28+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @NAT  # copy NAT to t10
        <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27 [- >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @t11  # move t11 into NAT
        < [- <- >] @t10                   # count NAT absent
        < + @t9                           # t9 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~30 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~29+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~28] @SYN  # copy SYN to t10
        <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~28 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~28+ <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~28] @t11  # move t11 into SYN
        < [- <- >] @t10                   # count SYN absent
        < + @t9                           # t9 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~35 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33] @BRIDGE  # copy BRIDGE to t10
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~33+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~33] @t11  # move t11 into BRIDGE
        < [- <- >] @t10                   # count BRIDGE absent
        < + @t9                           # t9 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>> ~36 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~35+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~34] @RVOID  # copy RVOID to t10
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~34+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~34] @t11  # move t11 into RVOID
        < [- <- >] @t10                   # count RVOID absent
        < + @t9                           # t9 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~37 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~36+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~35] @RPRIM  # copy RPRIM to t10
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~35 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~35+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~35] @t11  # move t11 into RPRIM
        < [- <- >] @t10                   # count RPRIM absent
        < + @t9                           # t9 plus 1
        << [- >>>+ >+ <<<<] @t7           # copy t7 to t10
        >>>> [- <<<<+ >>>>] @t11          # move t11 into t7
        < [- <- >] @t10                   # count t7 absent
        < + @t9                           # t9 plus 1
        < [- >>+ >+ <<<] @t8              # copy t8 to t10
        >>> [- <<<+ >>>] @t11             # move t11 into t8
        < [- <- >] @t10                   # count t8 absent
        # all conditions hold
        < [- >+ >+ <<] @t9                # copy t9
        >> [- <<+ >>] @t11                # move t11 into t9
        < ---------- ----- ~15 @t10       # subtract 15
        >> + @t12                         # assume equal
        << [ @t10                         # if different
            >> [-] @t12                   # not equal
            << [-] @t10                   # clear t10
        ] @t10
        >> [ @t12                         # if equal
            <<<<<<<<<< <<<<<< ~16 [-] @ELIG  # clear ELIG
            + @ELIG                       # ELIG plus 1
            >>>>>>>>>> >>>>>> ~16 [-] @t12  # clear t12
        ] @t12
        <<< [-] @t9                       # clear t9
        << [-] @t7                        # clear t7
        > [-] @t8                         # clear t8
        <<<<<<<<<< << ~12 [- >>>>>>>>>> ~10+ >+ <<<<<<<<<< < ~11] @ELIG  # copy ELIG to t6
        >>>>>>>>>> > ~11 [- <<<<<<<<<< < ~11+ >>>>>>>>>> > ~11] @t7  # move t7 into ELIG
        < [ @t6                           # eligible methods are scored
            # base: static direct 110 state copy 100 boolean 25 int 25
            <<<<<<<<< ~9 [-] @LO          # LO is 4
            ++++ @LO                      # LO plus 4
            > [-] @HI                     # HI is 1
            + @HI                         # HI plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~42+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~41] @KX  # copy KX to t7
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~41 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~41+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~41] @t8  # move t8 into KX
            + @t8                         # t8 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~41 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~40+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~39] @KX  # copy KX to t9
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~39 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~39+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~39] @t10  # move t10 into KX
            < [ @t9                       # if t9 then
                < [-] @t8                 # clear t8
                > [-] @t9                 # clear t9
            ] @t9
            < [ @t8                       # flag KX absent
                > [-] @t9                 # add 70
                ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ~70 @t9  # t9 plus 70
                [ @t9                     # while t9
                    - @t9                 # t9 minus 1
                    <<<<<<<<<< << ~12 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @LO  # copy LO
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t11  # move t11 into LO
                    > + @t12              # assume equal
                    << [ @t10             # if different
                        >> [-] @t12       # not equal
                        << [-] @t10       # clear t10
                    ] @t10
                    >> [ @t12             # if equal
                        <<<<<<<<<< <<<< ~14 + @HI  # HI plus 1
                        >>>>>>>>>> >>>> ~14 [-] @t12  # clear t12
                    ] @t12
                <<< ] @t9
                < [-] @t8                 # clear t8
            ] @t8
            < [ @t7                       # flag KX
                > [-] @t8                 # add 95
                ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++++ ~95 @t8  # t8 plus 95
                [ @t8                     # while t8
                    - @t8                 # t8 minus 1
                    <<<<<<<<<< < ~11 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >> ~12+ >+ <<<<<<<<<< <<< ~13] @LO  # copy LO
                    >>>>>>>>>> >>> ~13 [- <<<<<<<<<< <<< ~13+ >>>>>>>>>> >>> ~13] @t10  # move t10 into LO
                    > + @t11              # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        <<<<<<<<<< <<< ~13 + @HI  # HI plus 1
                        >>>>>>>>>> >>> ~13 [-] @t11  # clear t11
                    ] @t11
                <<< ] @t8
                < [-] @t7                 # clear t7
            ] @t7
            # five parameters
            <<<<<<<<<< <<<<<<<<<< ~20 [- >>>>>>>>>> >>>>>>>>>> > ~21+ >+ <<<<<<<<<< <<<<<<<<<< << ~22] @NPC  # copy NPC
            >>>>>>>>>> >>>>>>>>>> >> ~22 [- <<<<<<<<<< <<<<<<<<<< << ~22+ >>>>>>>>>> >>>>>>>>>> >> ~22] @t9  # move t9 into NPC
            < ----- @t8                   # subtract 5
            >> + @t10                     # assume equal
            << [ @t8                      # if different
                >> [-] @t10               # not equal
                << [-] @t8                # clear t8
            ] @t8
            >> [ @t10                     # if equal
                <<< [-] @t7               # clear t7
                + @t7                     # t7 plus 1
                >>> [-] @t10              # clear t10
            ] @t10
            <<< [- >+ >+ <<] @t7          # copy t7 to t8
            >> [- <<+ >>] @t9             # move t9 into t7
            + @t9                         # t9 plus 1
            << [- >>>+ >+ <<<<] @t7       # copy t7 to t10
            >>>> [- <<<<+ >>>>] @t11      # move t11 into t7
            < [ @t10                      # if t10 then
                < [-] @t9                 # clear t9
                > [-] @t10                # clear t10
            ] @t10
            < [ @t9                       # flag t7 absent
                > [-] @t10                # add 10
                ++++++++++ ~10 @t10       # t10 plus 10
                [ @t10                    # while t10
                    - @t10                # t10 minus 1
                    <<<<<<<<<< <<< ~13 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >>>> ~14+ >+ <<<<<<<<<< <<<<< ~15] @LO  # copy LO
                    >>>>>>>>>> >>>>> ~15 [- <<<<<<<<<< <<<<< ~15+ >>>>>>>>>> >>>>> ~15] @t12  # move t12 into LO
                    > + @t13              # assume equal
                    << [ @t11             # if different
                        >> [-] @t13       # not equal
                        << [-] @t11       # clear t11
                    ] @t11
                    >> [ @t13             # if equal
                        <<<<<<<<<< <<<<< ~15 + @HI  # HI plus 1
                        >>>>>>>>>> >>>>> ~15 [-] @t13  # clear t13
                    ] @t13
                <<< ] @t10
                < [-] @t9                 # clear t9
            ] @t9
            < [ @t8                       # flag t7
                > [-] @t9                 # add 30
                ++++++++++ ++++++++++ ++++++++++ ~30 @t9  # t9 plus 30
                [ @t9                     # while t9
                    - @t9                 # t9 minus 1
                    <<<<<<<<<< << ~12 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @LO  # copy LO
                    >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t11  # move t11 into LO
                    > + @t12              # assume equal
                    << [ @t10             # if different
                        >> [-] @t12       # not equal
                        << [-] @t10       # clear t10
                    ] @t10
                    >> [ @t12             # if equal
                        <<<<<<<<<< <<<< ~14 + @HI  # HI plus 1
                        >>>>>>>>>> >>>> ~14 [-] @t12  # clear t12
                    ] @t12
                <<< ] @t9
                < [-] @t8                 # clear t8
            ] @t8
            < [-] @t7                     # clear t7
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>> ~36 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~36+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~35] @SHORT  # copy SHORT to t7
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~35 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~35+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~35] @t8  # move t8 into SHORT
            < [ @t7                       # flag SHORT
                > [-] @t8                 # add 10
                ++++++++++ ~10 @t8        # t8 plus 10
                [ @t8                     # while t8
                    - @t8                 # t8 minus 1
                    <<<<<<<<<< < ~11 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >> ~12+ >+ <<<<<<<<<< <<< ~13] @LO  # copy LO
                    >>>>>>>>>> >>> ~13 [- <<<<<<<<<< <<< ~13+ >>>>>>>>>> >>> ~13] @t10  # move t10 into LO
                    > + @t11              # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        <<<<<<<<<< <<< ~13 + @HI  # HI plus 1
                        >>>>>>>>>> >>> ~13 [-] @t11  # clear t11
                    ] @t11
                <<< ] @t8
                < [-] @t7                 # clear t7
            ] @t7
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~45 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~45+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~44] @PUB  # copy PUB to t7
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~44 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~44+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~44] @t8  # move t8 into PUB
            < [ @t7                       # flag PUB
                > [-] @t8                 # add 8
                ++++++++ ~8 @t8           # t8 plus 8
                [ @t8                     # while t8
                    - @t8                 # t8 minus 1
                    <<<<<<<<<< < ~11 + @LO  # LO plus 1
                    # carry
                    [- >>>>>>>>>> >> ~12+ >+ <<<<<<<<<< <<< ~13] @LO  # copy LO
                    >>>>>>>>>> >>> ~13 [- <<<<<<<<<< <<< ~13+ >>>>>>>>>> >>> ~13] @t10  # move t10 into LO
                    > + @t11              # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        <<<<<<<<<< <<< ~13 + @HI  # HI plus 1
                        >>>>>>>>>> >>> ~13 [-] @t11  # clear t11
                    ] @t11
                <<< ] @t8
                < [-] @t7                 # clear t7
            ] @t7
            < [-] @t6                     # clear t6
        ] @t6
        [-] @t6                           # t6 is 1
        + @t6                             # t6 plus 1
        > [-] @t7                         # t7 is 73
        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++ ~73 @t7  # t7 plus 73
        <<<<<<<<< ~9 [- >>>>>>>>>> >>>> ~14+ >>+ <<<<<<<<<< <<<<<< ~16] @HI  # copy HI to t12
        >>>>>>>>>> >>>>>> ~16 [- <<<<<<<<<< <<<<<< ~16+ >>>>>>>>>> >>>>>> ~16] @t14  # move t14 into HI
        <<<<<<<< ~8 [- >>>>>>>+ >+ <<<<<<<< ~8] @t6  # copy t6 to t13
        >>>>>>>> ~8 [- <<<<<<<< ~8+ >>>>>>>> ~8] @t14  # move t14 into t6
        << [ @t12                         # count down
            # case t13 equals 0
            > [- >+ >+ <<] @t13           # copy t13
            >> [- <<+ >>] @t15            # move t15 into t13
            > + @t16                      # assume equal
            << [ @t14                     # if different
                >> [-] @t16               # not equal
                <<<< - @t12               # t12 minus 1
                > - @t13                  # t13 minus 1
                > [-] @t14                # clear t14
            ] @t14
            >> [ @t16                     # if equal
                <<<<<<< [-] @t9           # clear t9
                + @t9                     # t9 plus 1
                >>> [-] @t12              # clear t12
                >>>> [-] @t16             # clear t16
            ] @t16
        <<<< ] @t12
        > [-] @t13                        # clear t13
        <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>>> ~14+ >>+ <<<<<<<<<< <<<<<< ~16] @HI  # copy HI to t12
        >>>>>>>>>> >>>>>> ~16 [- <<<<<<<<<< <<<<<< ~16+ >>>>>>>>>> >>>>>> ~16] @t14  # move t14 into HI
        <<<<<<<< ~8 [- >>>>>>>+ >+ <<<<<<<< ~8] @t6  # copy t6 to t13
        >>>>>>>> ~8 [- <<<<<<<< ~8+ >>>>>>>> ~8] @t14  # move t14 into t6
        < [- <- >] @t13                   # subtract
        <<< + @t10                        # t10 plus 1
        >> [ @t12                         # if t12 then
            << [-] @t10                   # clear t10
            >> [-] @t12                   # clear t12
            [-] @t12                      # clear t12
        ] @t12
        <<<<<<<<<< <<<<< ~15 [- >>>>>>>>>> >>>>> ~15+ >>+ <<<<<<<<<< <<<<<<< ~17] @LO  # copy LO to t12
        >>>>>>>>>> >>>>>>> ~17 [- <<<<<<<<<< <<<<<<< ~17+ >>>>>>>>>> >>>>>>> ~17] @t14  # move t14 into LO
        <<<<<<< [- >>>>>>+ >+ <<<<<<<] @t7  # copy t7 to t13
        >>>>>>> [- <<<<<<<+ >>>>>>>] @t14  # move t14 into t7
        << [ @t12                         # count down
            # case t13 equals 0
            > [- >+ >+ <<] @t13           # copy t13
            >> [- <<+ >>] @t15            # move t15 into t13
            > + @t16                      # assume equal
            << [ @t14                     # if different
                >> [-] @t16               # not equal
                <<<< - @t12               # t12 minus 1
                > - @t13                  # t13 minus 1
                > [-] @t14                # clear t14
            ] @t14
            >> [ @t16                     # if equal
                <<<<< [-] @t11            # clear t11
                + @t11                    # t11 plus 1
                > [-] @t12                # clear t12
                >>>> [-] @t16             # clear t16
            ] @t16
        <<<< ] @t12
        > [-] @t13                        # clear t13
        <<< [- >>+ <<] @t10               # move t10 into t12
        > [- >+ <] @t11                   # move t11 into t12
        # high equal and low greater
        > [- >+ >+ <<] @t12               # copy t12
        >> [- <<+ >>] @t14                # move t14 into t12
        < -- @t13                         # subtract 2
        >> + @t15                         # assume equal
        << [ @t13                         # if different
            >> [-] @t15                   # not equal
            << [-] @t13                   # clear t13
        ] @t13
        >> [ @t15                         # if equal
            <<<<<< + @t9                  # t9 plus 1
            >>>>>> [-] @t15               # clear t15
        ] @t15
        <<< [-] @t12                      # clear t12
        <<< [ @t9                         # if t9 then
            < [-] @t8                     # clear t8
            + @t8                         # t8 plus 1
            > [-] @t9                     # clear t9
        ] @t9
        <<< [-] @t6                       # clear t6
        > [-] @t7                         # clear t7
        <<<<<<<<<< < ~11 [- >>>>>>>>>> >>> ~13+ >+ <<<<<<<<<< <<<< ~14] @ELIG  # copy ELIG to t9
        >>>>>>>>>> >>>> ~14 [- <<<<<<<<<< <<<< ~14+ >>>>>>>>>> >>>> ~14] @t10  # move t10 into ELIG
        << [- >+ >+ <<] @t8               # copy t8 to t9
        >> [- <<+ >>] @t10                # move t10 into t8
        # all conditions hold
        < [- >+ >+ <<] @t9                # copy t9
        >> [- <<+ >>] @t11                # move t11 into t9
        < -- @t10                         # subtract 2
        >> + @t12                         # assume equal
        << [ @t10                         # if different
            >> [-] @t12                   # not equal
            << [-] @t10                   # clear t10
        ] @t10
        >> [ @t12                         # if equal
            <<<<<<<<<< <<< ~13 [-] @ACC   # clear ACC
            + @ACC                        # ACC plus 1
            >>>>>>>>>> >>> ~13 [-] @t12   # clear t12
        ] @t12
        <<< [-] @t9                       # clear t9
        < [-] @t8                         # clear t8
        # response header
        << + @t6                          # major
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<<< ~26 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @OP  # copy OP to t6
        >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t7  # move t7 into OP
        < ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++ ~128 @t6  # opcode with the response bit
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        ++++ @t6                          # payload length low
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<< ~25 . @ID0  # echo request id
        > . @ID1                          # write ID1
        >>>>>>>>>> >>>> ~14 . @ELIG       # write ELIG
        > . @LO                           # write LO
        > . @HI                           # write HI
        > . @ACC                          # write ACC
        >>>>>> [-] @t5                    # clear t5
    ] @t5
    # opcode 51
    <<<<<<<<<< <<<<<<<<<< <<<<< ~25 [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @OP  # copy OP
    >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t4  # move t4 into OP
    < ---------- ---------- ---------- ---------- ---------- - ~51 @t3  # subtract 51
    >> + @t5                              # assume equal
    << [ @t3                              # if different
        >> [-] @t5                        # not equal
        << [-] @t3                        # clear t3
    ] @t3
    >> [ @t5                              # if equal
        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @UNH  # clear UNH
        # OP VIDEO SELECT
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~82 , @HAS  # any candidate
        [ @HAS                            # normalize HAS
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~60 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~60 [-] @HAS  # clear HAS
        ] @HAS
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~60 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~60+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~60] @t6  # move t6 into HAS
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~61 , @B0S  # best score low
        > , @B1S                          # best score high
        > , @HAS2                         # a second candidate
        [ @HAS2                           # normalize HAS2
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~63 + @t6  # t6 plus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~63 [-] @HAS2  # clear HAS2
        ] @HAS2
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~63 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~63+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~63] @t6  # move t6 into HAS2
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~64 , @S0S  # second score low
        > , @S1S                          # second score high
        # ambiguous when the best leads the second by less than 25
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~65 [-] @t6  # add 25
        ++++++++++ ++++++++++ +++++ ~25 @t6  # t6 plus 25
        [ @t6                             # while t6
            - @t6                         # t6 minus 1
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~64 + @S0S  # S0S plus 1
            # carry
            [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~63+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~62] @S0S  # copy S0S
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~62 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~62+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~62] @t8  # move t8 into S0S
            > + @t9                       # assume equal
            << [ @t7                      # if different
                >> [-] @t9                # not equal
                << [-] @t7                # clear t7
            ] @t7
            >> [ @t9                      # if equal
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~62 + @S1S  # S1S plus 1
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~62 [-] @t9  # clear t9
            ] @t9
        <<< ] @t6
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~65 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~60+ >>+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~58] @S1S  # copy S1S to t11
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~58 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~58+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~58] @t13  # move t13 into S1S
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @B1S  # copy B1S to t12
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t13  # move t13 into B1S
        << [ @t11                         # count down
            # case t12 equals 0
            > [- >+ >+ <<] @t12           # copy t12
            >> [- <<+ >>] @t14            # move t14 into t12
            > + @t15                      # assume equal
            << [ @t13                     # if different
                >> [-] @t15               # not equal
                <<<< - @t11               # t11 minus 1
                > - @t12                  # t12 minus 1
                > [-] @t13                # clear t13
            ] @t13
            >> [ @t15                     # if equal
                <<<<<<< [-] @t8           # clear t8
                + @t8                     # t8 plus 1
                >>> [-] @t11              # clear t11
                >>>> [-] @t15             # clear t15
            ] @t15
        <<<< ] @t11
        > [-] @t12                        # clear t12
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~59 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~60+ >>+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~58] @S1S  # copy S1S to t11
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~58 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~58+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~58] @t13  # move t13 into S1S
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @B1S  # copy B1S to t12
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t13  # move t13 into B1S
        < [- <- >] @t12                   # subtract
        <<< + @t9                         # t9 plus 1
        >> [ @t11                         # if t11 then
            << [-] @t9                    # clear t9
            >> [-] @t11                   # clear t11
            [-] @t11                      # clear t11
        ] @t11
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~59 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~59+ >>+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~57] @S0S  # copy S0S to t11
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~57 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~57+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~57] @t13  # move t13 into S0S
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54] @B0S  # copy B0S to t12
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~54 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~54] @t13  # move t13 into B0S
        << [ @t11                         # count down
            # case t12 equals 0
            > [- >+ >+ <<] @t12           # copy t12
            >> [- <<+ >>] @t14            # move t14 into t12
            > + @t15                      # assume equal
            << [ @t13                     # if different
                >> [-] @t15               # not equal
                <<<< - @t11               # t11 minus 1
                > - @t12                  # t12 minus 1
                > [-] @t13                # clear t13
            ] @t13
            >> [ @t15                     # if equal
                <<<<< [-] @t10            # clear t10
                + @t10                    # t10 plus 1
                > [-] @t11                # clear t11
                >>>> [-] @t15             # clear t15
            ] @t15
        <<<< ] @t11
        > [-] @t12                        # clear t12
        <<< [- >>+ <<] @t9                # move t9 into t11
        > [- >+ <] @t10                   # move t10 into t11
        # high equal and low greater
        > [- >+ >+ <<] @t11               # copy t11
        >> [- <<+ >>] @t13                # move t13 into t11
        < -- @t12                         # subtract 2
        >> + @t14                         # assume equal
        << [ @t12                         # if different
            >> [-] @t14                   # not equal
            << [-] @t12                   # clear t12
        ] @t12
        >> [ @t14                         # if equal
            <<<<<< + @t8                  # t8 plus 1
            >>>>>> [-] @t14               # clear t14
        ] @t14
        <<< [-] @t11                      # clear t11
        <<< [ @t8                         # if t8 then
            << [-] @t6                    # clear t6
            + @t6                         # t6 plus 1
            >> [-] @t8                    # clear t8
        ] @t8
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~61 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~61+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~60] @HAS2  # copy HAS2 to t8
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~60 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~60+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< ~60] @t9  # move t9 into HAS2
        <<< [- >>+ >+ <<<] @t6            # copy t6 to t8
        >>> [- <<<+ >>>] @t9              # move t9 into t6
        # all conditions hold
        < [- >+ >+ <<] @t8                # copy t8
        >> [- <<+ >>] @t10                # move t10 into t8
        < -- @t9                          # subtract 2
        >> + @t11                         # assume equal
        << [ @t9                          # if different
            >> [-] @t11                   # not equal
            << [-] @t9                    # clear t9
        ] @t9
        >> [ @t11                         # if equal
            <<<< [-] @t7                  # clear t7
            + @t7                         # t7 plus 1
            >>>> [-] @t11                 # clear t11
        ] @t11
        <<< [-] @t8                       # clear t8
        < [ @t7                           # ambiguous
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~65 [-] @D  # D becomes 3
            +++ @D                        # D plus 3
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~65 [-] @t7  # clear t7
        ] @t7
        < [-] @t6                         # clear t6
        [-] @t6                           # t6 is 1
        + @t6                             # t6 plus 1
        > [-] @t7                         # t7 is 73
        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ +++ ~73 @t7  # t7 plus 73
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~61 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >>+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54] @B1S  # copy B1S to t12
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~54 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~54] @t14  # move t14 into B1S
        <<<<<<<< ~8 [- >>>>>>>+ >+ <<<<<<<< ~8] @t6  # copy t6 to t13
        >>>>>>>> ~8 [- <<<<<<<< ~8+ >>>>>>>> ~8] @t14  # move t14 into t6
        << [ @t12                         # count down
            # case t13 equals 0
            > [- >+ >+ <<] @t13           # copy t13
            >> [- <<+ >>] @t15            # move t15 into t13
            > + @t16                      # assume equal
            << [ @t14                     # if different
                >> [-] @t16               # not equal
                <<<< - @t12               # t12 minus 1
                > - @t13                  # t13 minus 1
                > [-] @t14                # clear t14
            ] @t14
            >> [ @t16                     # if equal
                <<<<<<< [-] @t9           # clear t9
                + @t9                     # t9 plus 1
                >>> [-] @t12              # clear t12
                >>>> [-] @t16             # clear t16
            ] @t16
        <<<< ] @t12
        > [-] @t13                        # clear t13
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >>+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54] @B1S  # copy B1S to t12
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~54 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<< ~54] @t14  # move t14 into B1S
        <<<<<<<< ~8 [- >>>>>>>+ >+ <<<<<<<< ~8] @t6  # copy t6 to t13
        >>>>>>>> ~8 [- <<<<<<<< ~8+ >>>>>>>> ~8] @t14  # move t14 into t6
        < [- <- >] @t13                   # subtract
        <<< + @t10                        # t10 plus 1
        >> [ @t12                         # if t12 then
            << [-] @t10                   # clear t10
            >> [-] @t12                   # clear t12
            [-] @t12                      # clear t12
        ] @t12
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55+ >>+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53] @B0S  # copy B0S to t12
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~53 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~53] @t14  # move t14 into B0S
        <<<<<<< [- >>>>>>+ >+ <<<<<<<] @t7  # copy t7 to t13
        >>>>>>> [- <<<<<<<+ >>>>>>>] @t14  # move t14 into t7
        << [ @t12                         # count down
            # case t13 equals 0
            > [- >+ >+ <<] @t13           # copy t13
            >> [- <<+ >>] @t15            # move t15 into t13
            > + @t16                      # assume equal
            << [ @t14                     # if different
                >> [-] @t16               # not equal
                <<<< - @t12               # t12 minus 1
                > - @t13                  # t13 minus 1
                > [-] @t14                # clear t14
            ] @t14
            >> [ @t16                     # if equal
                <<<<< [-] @t11            # clear t11
                + @t11                    # t11 plus 1
                > [-] @t12                # clear t12
                >>>> [-] @t16             # clear t16
            ] @t16
        <<<< ] @t12
        > [-] @t13                        # clear t13
        <<< [- >>+ <<] @t10               # move t10 into t12
        > [- >+ <] @t11                   # move t11 into t12
        # high equal and low greater
        > [- >+ >+ <<] @t12               # copy t12
        >> [- <<+ >>] @t14                # move t14 into t12
        < -- @t13                         # subtract 2
        >> + @t15                         # assume equal
        << [ @t13                         # if different
            >> [-] @t15                   # not equal
            << [-] @t13                   # clear t13
        ] @t13
        >> [ @t15                         # if equal
            <<<<<< + @t9                  # t9 plus 1
            >>>>>> [-] @t15               # clear t15
        ] @t15
        <<< [-] @t12                      # clear t12
        <<< [ @t9                         # if t9 then
            < [-] @t8                     # clear t8
            + @t8                         # t8 plus 1
            > [-] @t9                     # clear t9
        ] @t9
        <<< [-] @t6                       # clear t6
        > [-] @t7                         # clear t7
        >> + @t9                          # t9 plus 1
        < [- >>+ >+ <<<] @t8              # copy t8 to t10
        >>> [- <<<+ >>>] @t11             # move t11 into t8
        < [ @t10                          # if t10 then
            < [-] @t9                     # clear t9
            > [-] @t10                    # clear t10
        ] @t10
        < [ @t9                           # below the threshold
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~63 [-] @D  # D becomes 2
            ++ @D                         # D plus 2
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~63 [-] @t9  # clear t9
        ] @t9
        < [-] @t8                         # clear t8
        << + @t6                          # t6 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~60 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~59+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~58] @HAS  # copy HAS to t7
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~58 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~58+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~58] @t8  # move t8 into HAS
        < [ @t7                           # if t7 then
            < [-] @t6                     # clear t6
            > [-] @t7                     # clear t7
        ] @t7
        < [ @t6                           # no candidate
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>> ~66 [-] @D  # D becomes 1
            + @D                          # D plus 1
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~66 [-] @t6  # clear t6
        ] @t6
        # response header
        + @t6                             # major
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<<< ~26 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @OP  # copy OP to t6
        >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t7  # move t7 into OP
        < ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++ ~128 @t6  # opcode with the response bit
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        + @t6                             # payload length low
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<< ~25 . @ID0  # echo request id
        > . @ID1                          # write ID1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~90 . @D  # write D
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~67 [-] @t5  # clear t5
    ] @t5
    # opcode 52
    <<<<<<<<<< <<<<<<<<<< <<<<< ~25 [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @OP  # copy OP
    >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t4  # move t4 into OP
    < ---------- ---------- ---------- ---------- ---------- -- ~52 @t3  # subtract 52
    >> + @t5                              # assume equal
    << [ @t3                              # if different
        >> [-] @t5                        # not equal
        << [-] @t3                        # clear t3
    ] @t3
    >> [ @t5                              # if equal
        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @UNH  # clear UNH
        # OP PROFILE: exact compatibility profile of the X version name
        > , @NP                           # first chunk length
        [ @NP                             # each chunk
            [ @NP                         # each character
                - @NP                     # NP minus 1
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>> ~76 , @CH  # read CH
                >> [-] @NVS               # NVS is 12
                ++++++++++ ++ ~12 @NVS    # NVS plus 12
                # version state machine
                # case VS equals 0
                < [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
                > + @t8                   # assume equal
                << [ @t6                  # if different
                    >> [-] @t8            # not equal
                    << [-] @t6            # clear t6
                ] @t6
                >> [ @t8                  # if equal
                    # character code 1
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < - @t9               # subtract 1
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 0
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    # character code 2
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~50 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < -- @t9              # subtract 2
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 0
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    # character code 7
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~50 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < ------- @t9         # subtract 7
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 1
                        + @NVS            # NVS plus 1
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    <<< [-] @t8           # clear t8
                ] @t8
                # case VS equals 1
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
                < - @t6                   # subtract 1
                >> + @t8                  # assume equal
                << [ @t6                  # if different
                    >> [-] @t8            # not equal
                    << [-] @t6            # clear t6
                ] @t6
                >> [ @t8                  # if equal
                    # character code 8
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < -------- ~8 @t9     # subtract 8
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 2
                        ++ @NVS           # NVS plus 2
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    <<< [-] @t8           # clear t8
                ] @t8
                # case VS equals 2
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
                < -- @t6                  # subtract 2
                >> + @t8                  # assume equal
                << [ @t6                  # if different
                    >> [-] @t8            # not equal
                    << [-] @t6            # clear t6
                ] @t6
                >> [ @t8                  # if equal
                    # character code 3
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < --- @t9             # subtract 3
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 3
                        +++ @NVS          # NVS plus 3
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    <<< [-] @t8           # clear t8
                ] @t8
                # case VS equals 3
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
                < --- @t6                 # subtract 3
                >> + @t8                  # assume equal
                << [ @t6                  # if different
                    >> [-] @t8            # not equal
                    << [-] @t6            # clear t6
                ] @t6
                >> [ @t8                  # if equal
                    # character code 13
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < ---------- --- ~13 @t9  # subtract 13
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 4
                        ++++ @NVS         # NVS plus 4
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    # character code 14
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~50 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < ---------- ---- ~14 @t9  # subtract 14
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 7
                        +++++++ @NVS      # NVS plus 7
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    <<< [-] @t8           # clear t8
                ] @t8
                # case VS equals 4
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
                < ---- @t6                # subtract 4
                >> + @t8                  # assume equal
                << [ @t6                  # if different
                    >> [-] @t8            # not equal
                    << [-] @t6            # clear t6
                ] @t6
                >> [ @t8                  # if equal
                    # character code 3
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < --- @t9             # subtract 3
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 5
                        +++++ @NVS        # NVS plus 5
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    <<< [-] @t8           # clear t8
                ] @t8
                # case VS equals 5
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
                < ----- @t6               # subtract 5
                >> + @t8                  # assume equal
                << [ @t6                  # if different
                    >> [-] @t8            # not equal
                    << [-] @t6            # clear t6
                ] @t6
                >> [ @t8                  # if equal
                    # character code 7
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < ------- @t9         # subtract 7
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 6
                        ++++++ @NVS       # NVS plus 6
                        > [-] @PID        # PID becomes 1
                        + @PID            # PID plus 1
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~53 [-] @t11  # clear t11
                    ] @t11
                    <<< [-] @t8           # clear t8
                ] @t8
                # case VS equals 6
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
                < ------ @t6              # subtract 6
                >> + @t8                  # assume equal
                << [ @t6                  # if different
                    >> [-] @t8            # not equal
                    << [-] @t6            # clear t6
                ] @t6
                >> [ @t8                  # if equal
                    # character code 2
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < -- @t9              # subtract 2
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 10
                        ++++++++++ ~10 @NVS  # NVS plus 10
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    # character code 3
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~50 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < --- @t9             # subtract 3
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 10
                        ++++++++++ ~10 @NVS  # NVS plus 10
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    # character code 4
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~50 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < ---- @t9            # subtract 4
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 10
                        ++++++++++ ~10 @NVS  # NVS plus 10
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    # character code 5
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~50 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < ----- @t9           # subtract 5
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 10
                        ++++++++++ ~10 @NVS  # NVS plus 10
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    # character code 1
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~50 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < - @t9               # subtract 1
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 11
                        ++++++++++ + ~11 @NVS  # NVS plus 11
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    <<< [-] @t8           # clear t8
                ] @t8
                # case VS equals 7
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
                < ------- @t6             # subtract 7
                >> + @t8                  # assume equal
                << [ @t6                  # if different
                    >> [-] @t8            # not equal
                    << [-] @t6            # clear t6
                ] @t6
                >> [ @t8                  # if equal
                    # character code 3
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < --- @t9             # subtract 3
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 8
                        ++++++++ ~8 @NVS  # NVS plus 8
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    <<< [-] @t8           # clear t8
                ] @t8
                # case VS equals 8
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
                < -------- ~8 @t6         # subtract 8
                >> + @t8                  # assume equal
                << [ @t6                  # if different
                    >> [-] @t8            # not equal
                    << [-] @t6            # clear t6
                ] @t6
                >> [ @t8                  # if equal
                    # character code 6
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < ------ @t9          # subtract 6
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 9
                        +++++++++ ~9 @NVS  # NVS plus 9
                        > [-] @PID        # PID becomes 2
                        ++ @PID           # PID plus 2
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<< ~53 [-] @t11  # clear t11
                    ] @t11
                    <<< [-] @t8           # clear t8
                ] @t8
                # case VS equals 9
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
                < --------- ~9 @t6        # subtract 9
                >> + @t8                  # assume equal
                << [ @t6                  # if different
                    >> [-] @t8            # not equal
                    << [-] @t6            # clear t6
                ] @t6
                >> [ @t8                  # if equal
                    # character code 2
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < -- @t9              # subtract 2
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 10
                        ++++++++++ ~10 @NVS  # NVS plus 10
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    # character code 3
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~50 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < --- @t9             # subtract 3
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 10
                        ++++++++++ ~10 @NVS  # NVS plus 10
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    # character code 4
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~50 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < ---- @t9            # subtract 4
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 10
                        ++++++++++ ~10 @NVS  # NVS plus 10
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    # character code 5
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~50 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < ----- @t9           # subtract 5
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 10
                        ++++++++++ ~10 @NVS  # NVS plus 10
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    # character code 1
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~50 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < - @t9               # subtract 1
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 11
                        ++++++++++ + ~11 @NVS  # NVS plus 11
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    <<< [-] @t8           # clear t8
                ] @t8
                # case VS equals 10
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
                < ---------- ~10 @t6      # subtract 10
                >> + @t8                  # assume equal
                << [ @t6                  # if different
                    >> [-] @t8            # not equal
                    << [-] @t6            # clear t6
                ] @t6
                >> [ @t8                  # if equal
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55 [-] @NVS  # NVS becomes 10
                    ++++++++++ ~10 @NVS   # NVS plus 10
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [-] @t8  # clear t8
                ] @t8
                # case VS equals 11
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
                <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
                < ---------- - ~11 @t6    # subtract 11
                >> + @t8                  # assume equal
                << [ @t6                  # if different
                    >> [-] @t8            # not equal
                    << [-] @t6            # clear t6
                ] @t6
                >> [ @t8                  # if equal
                    # character code 1
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>> ~53 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < - @t9               # subtract 1
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 11
                        ++++++++++ + ~11 @NVS  # NVS plus 11
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    # character code 2
                    >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> ~50 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51] @CH  # copy CH
                    <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> > ~51+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< < ~51] @t10  # move t10 into CH
                    < -- @t9              # subtract 2
                    >> + @t11             # assume equal
                    << [ @t9              # if different
                        >> [-] @t11       # not equal
                        << [-] @t9        # clear t9
                    ] @t9
                    >> [ @t11             # if equal
                        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~52 [-] @NVS  # NVS becomes 11
                        ++++++++++ + ~11 @NVS  # NVS plus 11
                        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< << ~52 [-] @t11  # clear t11
                    ] @t11
                    <<< [-] @t8           # clear t8
                ] @t8
                >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [-] @VS  # clear VS
                > [- <+ >] @NVS           # VS takes NVS
                << [-] @CH                # clear CH
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~76 ] @NP
            , @NP                         # next chunk length
        ] @NP
        # matched states
        # case VS equals 6
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~77 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
        < ------ @t6                      # subtract 6
        >> + @t8                          # assume equal
        << [ @t6                          # if different
            >> [-] @t8                    # not equal
            << [-] @t6                    # clear t6
        ] @t6
        >> [ @t8                          # if equal
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~57 [-] @M  # clear M
            + @M                          # M plus 1
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~57 [-] @t8  # clear t8
        ] @t8
        # case VS equals 9
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
        < --------- ~9 @t6                # subtract 9
        >> + @t8                          # assume equal
        << [ @t6                          # if different
            >> [-] @t8                    # not equal
            << [-] @t6                    # clear t6
        ] @t6
        >> [ @t8                          # if equal
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~57 [-] @M  # clear M
            + @M                          # M plus 1
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~57 [-] @t8  # clear t8
        ] @t8
        # case VS equals 10
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
        < ---------- ~10 @t6              # subtract 10
        >> + @t8                          # assume equal
        << [ @t6                          # if different
            >> [-] @t8                    # not equal
            << [-] @t6                    # clear t6
        ] @t6
        >> [ @t8                          # if equal
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~57 [-] @M  # clear M
            + @M                          # M plus 1
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~57 [-] @t8  # clear t8
        ] @t8
        # case VS equals 11
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>> ~54 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<< ~56+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55] @VS  # copy VS
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>> ~55+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<< ~55] @t7  # move t7 into VS
        < ---------- - ~11 @t6            # subtract 11
        >> + @t8                          # assume equal
        << [ @t6                          # if different
            >> [-] @t8                    # not equal
            << [-] @t6                    # clear t6
        ] @t6
        >> [ @t8                          # if equal
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~57 [-] @M  # clear M
            + @M                          # M plus 1
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~57 [-] @t8  # clear t8
        ] @t8
        << + @t6                          # t6 plus 1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>> ~59 [- <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~58+ >+ >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~57] @M  # copy M to t7
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~57 [- >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>> ~57+ <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<< ~57] @t8  # move t8 into M
        < [ @t7                           # if t7 then
            < [-] @t6                     # clear t6
            > [-] @t7                     # clear t7
        ] @t7
        < [ @t6                           # no profile
            >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>> ~58 [-] @PID  # clear PID
            <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<< ~58 [-] @t6  # clear t6
        ] @t6
        # response header
        + @t6                             # major
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<<< ~26 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @OP  # copy OP to t6
        >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t7  # move t7 into OP
        < ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++ ~128 @t6  # opcode with the response bit
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        + @t6                             # payload length low
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<< ~25 . @ID0  # echo request id
        > . @ID1                          # write ID1
        >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >>>>>>>>>> >> ~82 . @PID  # write PID
        <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<<< <<<<<<<<< ~59 [-] @t5  # clear t5
    ] @t5
    # opcode 53
    <<<<<<<<<< <<<<<<<<<< <<<<< ~25 [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @OP  # copy OP
    >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t4  # move t4 into OP
    < ---------- ---------- ---------- ---------- ---------- --- ~53 @t3  # subtract 53
    >> + @t5                              # assume equal
    << [ @t3                              # if different
        >> [-] @t5                        # not equal
        << [-] @t3                        # clear t3
    ] @t3
    >> [ @t5                              # if equal
        <<<<<<<<<< <<<<<<<<<< < ~21 [-] @UNH  # clear UNH
        # OP LIMITS: bounds the host applies to discovery and traversal
        # response header
        >>>>>>>>>> >>>>>>>>>> >> ~22 + @t6  # major
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<<< ~26 [- >>>>>>>>>> >>>>>>>>>> >>>>>> ~26+ >+ <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27] @OP  # copy OP to t6
        >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27 [- <<<<<<<<<< <<<<<<<<<< <<<<<<< ~27+ >>>>>>>>>> >>>>>>>>>> >>>>>>> ~27] @t7  # move t7 into OP
        < ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++ ~128 @t6  # opcode with the response bit
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        ++++++++++ ++ ~12 @t6             # payload length low
        . @t6                             # write t6
        [-] @t6                           # clear t6
        . @t6                             # write t6
        [-] @t6                           # clear t6
        <<<<<<<<<< <<<<<<<<<< <<<<< ~25 . @ID0  # echo request id
        > . @ID1                          # write ID1
        >>>>>>>>>> >>>>>>>>>> >>>> ~24 ++++++++++ ++ ~12 @t6  # witness window
        . @t6                             # write t6
        [-] @t6                           # clear t6
        ++++++++++ ++++++ ~16 @t6         # max boundaries
        . @t6                             # write t6
        [-] @t6                           # clear t6
        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++ ~64 @t6  # max inlined callers
        . @t6                             # write t6
        [-] @t6                           # clear t6
        +++++ @t6                         # scan depth
        . @t6                             # write t6
        [-] @t6                           # clear t6
        ---------- ---------- ---------- ---------- ---------- ---------- ---------- ---------- ---------- ------ ~96 @t6  # scan objects
        . @t6                             # write t6
        [-] @t6                           # clear t6
        ++++++++++ ++++++++++ ++++ ~24 @t6  # scan collection items
        . @t6                             # write t6
        [-] @t6                           # clear t6
        ++++++++++ ++++++++++ ++++++++++ ++++++++++ ++++++++ ~48 @t6  # stack frames
        . @t6                             # write t6
        [-] @t6                           # clear t6
        +++++ @t6                         # candidates logged
        . @t6                             # write t6
        [-] @t6                           # clear t6
        ---------- ---------- ---------- ---------- ---------- ---------- ~60 @t6  # reflection classes low
        . @t6                             # write t6
        [-] @t6                           # clear t6
        +++++++++ ~9 @t6                  # reflection classes high
        . @t6                             # write t6
        [-] @t6                           # clear t6
        ---------- -- ~12 @t6             # video classes low
        . @t6                             # write t6
        [-] @t6                           # clear t6
        + @t6                             # video classes high
        . @t6                             # write t6
        [-] @t6                           # clear t6
        < [-] @t5                         # clear t5
    ] @t5
    <<<<<<<<<< <<<<<<<<<< < ~21 [ @UNH    # unknown opcode
        # response header
        >>>>>>>>>> >>>>>>>>> ~19 + @t3    # major
        . @t3                             # write t3
        [-] @t3                           # clear t3
        . @t3                             # write t3
        [-] @t3                           # clear t3
        <<<<<<<<<< <<<<<<<<<< <<< ~23 [- >>>>>>>>>> >>>>>>>>>> >>> ~23+ >+ <<<<<<<<<< <<<<<<<<<< <<<< ~24] @OP  # copy OP to t3
        >>>>>>>>>> >>>>>>>>>> >>>> ~24 [- <<<<<<<<<< <<<<<<<<<< <<<< ~24+ >>>>>>>>>> >>>>>>>>>> >>>> ~24] @t4  # move t4 into OP
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
        <<<<<<<<<< <<<<<<<<<< << ~22 . @ID0  # echo request id
        > . @ID1                          # write ID1
        >> [-] @UNH                       # clear UNH
    ] @UNH
    >>>>>>>>>> >>>>>>>> ~18 [-] @t2       # clear t2
] @t2
