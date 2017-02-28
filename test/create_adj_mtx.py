import pandas as pd
import networkx as nx
import matplotlib.pyplot as plt
import pygraphviz
from networkx.drawing.nx_agraph import graphviz_layout
from networkx.drawing.nx_agraph import to_agraph
input_data = pd.read_csv('test_adj_mtx.csv', index_col=0)
G = nx.DiGraph(input_data.values).reverse()
pos = graphviz_layout(G)
pos = to_agraph(G)
pos.layout('dot')
pos.draw('test.png')
