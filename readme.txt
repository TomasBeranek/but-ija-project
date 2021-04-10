A warehouse simulation

The simulation demonstrates an order handling system in a customizable user-defined
warehouse. The carts are using the Floyd–Warshall algorithm for finding the shortest
path between shelves with the goods and the dispensing point. Each order has assigned
a single cart. An order can be specified in order.json file or GUI of a simulation.
The simulation supports interactive interventions like - closing an alley, adding
an order and changing the speed or the time of the simulation. The system is fully
customizable, a user can define a custom floor plan, orders and goods. The system
consists of:
  -- dispensing point
  -- shelves
  -- carts
  -- path nodes


Limitations
  -- a shelf is accessible only from a single node
  -- on a single shelf can be only one type of goods
  -- the system does not take into account the weight or volume of the goods
  -- a warehouse can only have a single dispensing point
  -- the carts cannot collide with each other


Input data

The simulation loads the data from files in a JSON format stored in data/:
  -- warehouse.json -- the floor plan of the warehouse
  -- orders.json    -- the specification of orders (time, goods, amount)
  -- goods.json     -- the placement of goods in shelves
An example of each file can be found in data/.


Requirements
  -- Ant (>= 1.10.7)
  -- Java SE 8 (or OpenJDK|Java SE >8 with JavaFX)


Usage

Use the ant tool to run 'ant run' or build 'ant compile' the project from the root
directory. Cleaning the project can be done by 'ant clean'.


Authors

Tomáš Beránek (xberan46)
Šimon Slobodník (xlobo06)


Project assignment

The project assignment can be found on:
  https://wis.fit.vutbr.cz/FIT/st/cwk.php.cs?title=Main_Page&csid=735862&id=13987


Repository

The up2date sources can be found in the following repository:
  https://github.com/TomasBeranek/but-ija-project
