#!/usr/bin/python
from random import randint, sample

NB_NODES = 30
NB_MSGS = 150

f = open('test2', 'w+')

for i in range(1, NB_NODES+1):
    # for j in range(i+1,NB_NODES):
    #     f.write(str(i) + ' ' + str(j) + ' ' + str(randint(1, 70)) + '\n')
    if i > 1:
        f.write(str(i) + ' ' + str(i-1) + ' ' + str(randint(1, 30)) + '\n')
    else:
        f.write(str(i) + ' ' + str(NB_NODES) +
                ' ' + str(randint(1, 70)) + '\n')

    l = sample(range(1, NB_NODES), 5)
    while i in l or (i-1) in l:
        l = sample(range(1, NB_NODES), 5)
    for j in l:
        f.write(str(i) + ' ' + str(j) + ' ' + str(randint(1, 30)) + '\n')

time = sample(range(200, 500), NB_MSGS)
time.sort()
for t in time:
    n2 = n1 = randint(1, NB_NODES-1)
    while n2 == n1:
        n2 = randint(1, NB_NODES-1)
    f.write('M ' + str(t) + ' ' + str(n1) + ' ' + str(n2) + ' ciao \n')

# for i in range(NB_MSGS):
#     time = 300 + randint(1, 100)
#     n2 = n1 = randint(1, NB_NODES-1)
#     while n2 == n1:
#         n2 = randint(1, NB_NODES-1)
#     f.write('M ' + str(time) + ' ' + str(n1) + ' ' + str(n2) + ' ciao \n')
