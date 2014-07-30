package com.mycompany.samples

/**
 * A class to do some stuff
 */
class MyClass < ParentClass
  #The constructor method
  def initialize
    l = [1,2,3]
    l.each do |i|
      puts i
    end
  end
end